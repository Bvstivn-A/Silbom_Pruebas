"""Pruebas de la interfaz real con una API HTTP controlada (sin base de producción)."""
import base64
import copy
import json
import os
from pathlib import Path
import socket
import struct
import threading
import time
import unittest
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit
from playwright.sync_api import sync_playwright, expect

STATIC = Path(os.environ.get('SILBOM_STATIC_ROOT', Path(__file__).resolve().parents[2] / 'src/main/resources/static'))
MACHINE = {'id': 'EQ-ENV-01', 'nombre': 'Equipo de prueba', 'area': 'Taller', 'icono': 'X',
           'bloqueada': False, 'bloqueosActivos': 0, 'horasOperativas': 100, 'fallasCorrectivas': 0, 'mtbf': None}


class Scenario:
    def __init__(self):
        self.machine = copy.deepcopy(MACHINE)
        self.tickets = []
        self.keys = {}
        self.requests = []
        self.events = []
        self.post_fault = None
        self.put_fault = None
        self.reads_fail = False
        self.lock = threading.Lock()


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(STATIC), **kwargs)

    def end_headers(self):
        self.send_header('Content-Security-Policy', "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' https: http://localhost:* http://127.0.0.1:*; worker-src 'self'")
        super().end_headers()

    def log_message(self, *_):
        pass

    def reply(self, status, body):
        data = json.dumps(body).encode() if not isinstance(body, bytes) else body
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        try:
            self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def drop(self):
        self.close_connection = True
        try:
            self.connection.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        self.connection.close()

    def api(self):
        path = urlsplit(self.path).path.removeprefix('/api/v1')
        state = self.server.scenario
        method = self.command
        auth = self.headers.get('Authorization', '')
        username = base64.b64decode(auth[6:]).decode().split(':', 1)[0] if auth.startswith('Basic ') else None
        key = self.headers.get('Idempotency-Key')
        state.requests.append({'method': method, 'path': path, 'key': key, 'authenticated': bool(auth)})
        if method == 'GET':
            if state.reads_fail:
                return self.reply(503, {'mensaje': 'Lectura temporalmente no disponible'})
            if path == '/maquinas':
                return self.reply(200, [state.machine])
            if not username:
                return self.reply(401, {'mensaje': 'Credencial inválida'})
            if path == '/tecnicos':
                return self.reply(200, [{'id': 1, 'username': username, 'nombre': 'Supervisor', 'rol': 'SUPERVISOR'}])
            if path == '/bloqueos':
                return self.reply(200, state.tickets)
            if path.startswith('/bloqueos/'):
                ticket = next((t for t in state.tickets if t['id'] == int(path.split('/')[2])), None)
                return self.reply(200 if ticket else 404, ticket)
            return self.reply(404, {})
        if not username:
            return self.reply(401, {'mensaje': 'Credencial inválida'})
        fault = state.post_fault if method == 'POST' else state.put_fault
        if method == 'POST':
            state.post_fault = None
        else:
            state.put_fault = None
        fault = fault or {}
        if fault.get('delay'):
            time.sleep(fault['delay'])
        if fault.get('drop_before'):
            return self.drop()
        if 'status' in fault and fault['status'] < 500:
            return self.reply(fault['status'], {'mensaje': 'Rechazado por prueba'})
        if fault.get('malformed'):
            return self.reply(201 if method == 'POST' else 200, b'<html>proxy</html>')
        if fault.get('incomplete'):
            return self.reply(201 if method == 'POST' else 200, {})
        with state.lock:
            if method == 'POST':
                payload = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))))
                if (username, key) in state.keys:
                    ticket = state.keys[username, key]
                else:
                    ticket = dict(payload, id=len(state.tickets) + 1, estado='ACTIVO', nombreTecnico=username,
                                  tecnicoId=1, horaInicio='2026-11-13T12:00:00', notasAuditoria=payload.get('notasConfidenciales'))
                    state.tickets.append(ticket)
                    state.keys[username, key] = ticket
                    state.machine['bloqueosActivos'] += 1
                    state.machine['bloqueada'] = True
                    state.events.append('CREADO')
                status = 201
            else:
                ticket = next(t for t in state.tickets if t['id'] == int(path.split('/')[2]))
                if ticket['estado'] == 'CERRADO':
                    return self.reply(409, {'mensaje': 'Ya cerrado'})
                ticket.update(estado='CERRADO', duracionMinutos=1)
                state.machine['bloqueosActivos'] -= 1
                state.machine['bloqueada'] = state.machine['bloqueosActivos'] > 0
                state.events.append('LIBERADO')
                status = 200
            if fault.get('drop_after'):
                return self.drop()
            if fault.get('fail_reads'):
                state.reads_fail = True
            return self.reply(fault.get('status', status), ticket)

    def do_GET(self):
        if self.path.startswith('/api/v1/'):
            return self.api()
        return super().do_GET()

    def do_POST(self):
        return self.api()

    def do_PUT(self):
        return self.api()


class MobileTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        cls.server.daemon_threads = True
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.origin = f'http://127.0.0.1:{cls.server.server_port}'
        cls.pw = sync_playwright().start()
        cls.engine = os.environ.get('SILBOM_BROWSER', 'chromium')
        launch = {}
        if cls.engine == 'chromium':
            launch['args'] = ['--no-sandbox', '--disable-dev-shm-usage']
            if os.environ.get('CHROMIUM_PATH'):
                launch['executable_path'] = os.environ['CHROMIUM_PATH']
        cls.browser = getattr(cls.pw, cls.engine).launch(**launch)

    @classmethod
    def tearDownClass(cls):
        cls.browser.close()
        cls.pw.stop()
        cls.server.shutdown()
        cls.server.server_close()

    def setUp(self):
        self.state = Scenario()
        self.server.scenario = self.state
        device = dict(self.pw.devices['iPhone 13']) if self.engine == 'webkit' else dict(viewport={'width':390,'height':844}, is_mobile=True, has_touch=True)
        device.pop('default_browser_type', None)
        self.context = self.browser.new_context(**device,
            service_workers='allow' if self._testMethodName.startswith('test_pwa_') else 'block')
        self.page = self.context.new_page()
        self.page.set_default_timeout(5000)
        self.errors = []
        self.page.on('pageerror', lambda err: self.errors.append(str(err)))
        self.page.goto(self.origin + '/app/index.html', wait_until='networkidle')

    def tearDown(self):
        self.context.close()
        self.assertEqual([], self.errors)

    def login(self):
        self.page.locator('#mnav-tecnicos').click()
        self.page.locator('#mobile-auth-user').fill('supervisor-test')
        self.page.locator('#mobile-auth-pass').fill('browser-test-password')
        self.page.locator('form[onsubmit*="loginFromMobilePanel"] button[type=submit]' ).click()
        self.page.wait_for_function('() => AppState.auth?.rol === "SUPERVISOR"')
        self.page.locator('#mnav-operar').click()

    def prepare(self):
        self.login()
        self.page.locator('#manual-machine-select').select_option('EQ-ENV-01')
        self.page.locator('#card-type-preventive').click()
        self.page.locator('#check-padlock').check()

    def create(self):
        self.prepare()
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('confirmado por el servidor')

    def assert_client_active(self):
        self.assertTrue(self.page.evaluate('AppState.machines[0].isLocked'))
        self.assertEqual('ACTIVO', self.page.evaluate('AppState.tickets[0].estado'))

    def test_offline_creation_has_no_local_success(self):
        self.prepare()
        self.state.post_fault = {'drop_before': True}
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Alta sin confirmar')
        self.assertEqual([], self.state.tickets)
        self.assertEqual(0, self.page.evaluate('AppState.tickets.length'))
        self.assertFalse(self.page.evaluate('AppState.machines[0].isLocked'))
        expect(self.page.locator('#tech-step-2')).to_be_visible()

    def test_offline_release_preserves_active_ticket(self):
        self.create()
        self.state.put_fault = {'drop_before': True}
        self.page.locator('#btn-unlock-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Liberación sin confirmar')
        self.assert_client_active()
        self.assertEqual('ACTIVO', self.state.tickets[0]['estado'])

    def test_lost_create_response_retries_same_key_once(self):
        self.prepare()
        self.state.post_fault = {'drop_after': True}
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Alta sin confirmar')
        self.assertEqual(1, len(self.state.tickets))
        self.assertEqual(0, self.page.evaluate('AppState.tickets.length'))
        self.page.locator('#btn-retry-operation').click()
        expect(self.page.locator('#operation-status')).to_contain_text('confirmado por el servidor')
        posts = [r for r in self.state.requests if r['method'] == 'POST']
        self.assertEqual(2, len(posts))
        self.assertEqual(posts[0]['key'], posts[1]['key'])
        self.assertEqual(['CREADO'], self.state.events)
        self.assertEqual(1, self.state.machine['bloqueosActivos'])

    def test_double_submit_sends_only_one_request(self):
        self.prepare()
        self.state.post_fault = {'delay': .2}
        self.page.locator('#btn-submit-lock').click(click_count=2)
        expect(self.page.locator('#operation-status')).to_contain_text('confirmado por el servidor')
        self.assertEqual(1, sum(r['method'] == 'POST' for r in self.state.requests))
        self.assertEqual(1, self.state.machine['bloqueosActivos'])

    def test_lost_release_response_reconciles_409_with_server(self):
        self.create()
        self.state.put_fault = {'drop_after': True}
        self.page.locator('#btn-unlock-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Liberación sin confirmar')
        self.assert_client_active()
        self.assertEqual('CERRADO', self.state.tickets[0]['estado'])
        self.page.locator('#btn-retry-operation').click()
        expect(self.page.locator('#operation-status')).to_contain_text('cero bloqueos digitales')
        self.assertEqual(['CREADO', 'LIBERADO'], self.state.events)
        self.assertEqual(0, self.state.machine['bloqueosActivos'])
        self.assertEqual('CERRADO', self.page.evaluate('AppState.tickets[0].estado'))

    def test_release_keeps_machine_blocked_with_second_ticket(self):
        self.create()
        other = dict(self.state.tickets[0], id=2, tecnicoId=2, nombreTecnico='otro-tecnico')
        self.state.tickets.append(other)
        self.state.machine.update(bloqueosActivos=2, bloqueada=True)
        self.page.locator('#btn-unlock-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('sigue bloqueada (1 activo(s))')
        self.assertTrue(self.page.evaluate('AppState.machines[0].isLocked'))
        self.assertEqual(1, self.state.machine['bloqueosActivos'])

    def test_confirmed_release_with_failed_refresh_never_claims_machine_free(self):
        self.create()
        self.state.put_fault = {'fail_reads': True}
        self.page.locator('#btn-unlock-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('CERRADO confirmado. Estado del equipo sin verificar')
        self.assertTrue(self.page.evaluate('AppState.machines[0].isLocked'))
        self.assertEqual('CERRADO', self.page.evaluate('AppState.tickets[0].estado'))

    def test_http_rejections_never_confirm_creation(self):
        self.prepare()
        for status in (401, 403, 409):
            with self.subTest(status=status):
                self.state.post_fault = {'status': status}
                self.page.locator('#btn-submit-lock').click()
                expect(self.page.locator('#operation-status')).to_contain_text(f'El servidor respondió {status}')
                self.assertEqual([], self.state.tickets)
                self.assertEqual(0, self.page.evaluate('AppState.tickets.length'))

    def test_500_after_commit_is_uncertain_and_retry_does_not_duplicate(self):
        self.prepare()
        self.state.post_fault = {'status': 500}
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Alta sin confirmar')
        self.page.locator('#btn-retry-operation').click()
        expect(self.page.locator('#operation-status')).to_contain_text('confirmado por el servidor')
        self.assertEqual(1, len(self.state.tickets))

    def test_invalid_success_body_never_confirms_creation(self):
        self.prepare()
        self.state.post_fault = {'incomplete': True}
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Alta sin confirmar')
        self.assertEqual(0, self.page.evaluate('AppState.tickets.length'))

    def test_malformed_json_is_uncertain(self):
        self.prepare()
        self.state.post_fault = {'malformed': True}
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Alta sin confirmar')
        self.assertEqual([], self.state.tickets)

    def test_timeout_can_retry_without_duplicate(self):
        self.prepare()
        self.page.evaluate('''() => {
          const original = SilbomHttp.request;
          window.originalRequest = original;
          SilbomHttp.request = (endpoint, opts) => original(endpoint, {...opts, timeoutMs: 50});
        }''')
        self.state.post_fault = {'delay': .2}
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Alta sin confirmar')
        self.page.evaluate('SilbomHttp.request = window.originalRequest')
        self.page.wait_for_timeout(250)
        self.page.locator('#btn-retry-operation').click()
        expect(self.page.locator('#operation-status')).to_contain_text('confirmado por el servidor')
        self.assertEqual(1, len(self.state.tickets))

    def test_deep_link_identifies_only_and_does_not_change_api_origin(self):
        self.page.goto(self.origin + '/app/?maquina=EQ-ENV-01&api=https://attacker.invalid', wait_until='networkidle')
        expect(self.page.locator('#tech-step-2')).to_be_visible()
        self.assertEqual('EQ-ENV-01', self.page.locator('#form-machine-code').inner_text())
        self.assertEqual('/api/v1', self.page.evaluate('getApiBaseUrl()'))
        self.assertFalse(any(r['method'] != 'GET' for r in self.state.requests))
        expect(self.page.locator('#btn-submit-lock')).to_be_disabled()

    def test_invalid_and_unknown_links_do_not_select_another_machine(self):
        for machine in ('bad-code', 'EQ-UNKNOWN-01', 'EQ-ZZ-99', ''):
            with self.subTest(machine=machine):
                self.page.goto(self.origin + '/app/?maquina=' + machine, wait_until='networkidle')
                expect(self.page.locator('#tech-step-1')).to_be_visible()
                self.assertIsNone(self.page.evaluate('AppState.selectedMachineId'))
                self.assertFalse(any(r['method'] != 'GET' for r in self.state.requests))

    def test_non_default_port_and_https_resolution(self):
        self.assertEqual('/api/v1', self.page.evaluate('getApiBaseUrl()'))
        self.assertEqual('https://plant.example:8443/api/v1', self.page.evaluate("SilbomHttp.baseUrl('https://plant.example:8443')"))
        self.assertFalse(self.page.evaluate("SilbomHttp.allowsCredentials('http://192.168.1.50:8080')"))
        self.page.goto(self.origin + '/', wait_until='networkidle')
        self.assertEqual('/api/v1', self.page.evaluate('API_BASE_URL'))
        self.assertTrue(self.page.evaluate('AppState.apiOnline'))

    def test_supervisor_does_not_present_failed_catalog_as_operational(self):
        self.page.goto(self.origin + '/', wait_until='networkidle')
        self.state.reads_fail = True
        self.page.evaluate('() => runSupervisorPollTick()')
        expect(self.page.locator('#kpi-active-count')).to_contain_text('Sin verificar')
        expect(self.page.locator('#kpi-plant-tag')).to_contain_text('Estado actual sin verificar')

    def test_mobile_viewports_and_long_machine_name(self):
        self.state.machine['nombre'] = 'Equipo industrial ' + 'X' * 80
        self.page.reload(wait_until='networkidle')
        self.page.locator('#manual-machine-select').select_option('EQ-ENV-01')
        for width in (320, 360, 390, 768):
            with self.subTest(width=width):
                self.page.set_viewport_size({'width': width, 'height': 640})
                self.assertLessEqual(self.page.evaluate('document.documentElement.scrollWidth'), width)
                expect(self.page.locator('#btn-submit-lock')).to_be_visible()

    def test_ios_manual_install_and_apple_icon(self):
        self.page.evaluate("() => Object.defineProperty(navigator, 'userAgent', {value:'iPhone Safari', configurable:true})")
        self.page.locator('#btn-open-install-modal').click()
        expect(self.page.locator('#install-app-modal')).to_contain_text('Compartir → Añadir a pantalla de inicio')
        expect(self.page.locator('#btn-native-pwa-install')).to_be_hidden()
        self.assertEqual('SILBOM', self.page.locator('meta[name="apple-mobile-web-app-title"]').get_attribute('content'))
        icon = self.page.locator('link[rel="apple-touch-icon"]').get_attribute('href')
        response = self.page.request.get(self.origin + '/app/' + icon.removeprefix('./'))
        self.assertEqual((180, 180), struct.unpack('>II', response.body()[16:24]))
        self.page.evaluate("() => Object.defineProperty(navigator, 'standalone', {value:true, configurable:true})")
        self.page.evaluate('() => openInstallModal()')
        expect(self.page.locator('#pwa-install-status')).to_contain_text('ya está abierta')

    def test_online_event_requires_api_verification(self):
        self.prepare()
        self.page.evaluate("() => window.dispatchEvent(new Event('offline'))")
        expect(self.page.locator('#btn-submit-lock')).to_be_disabled()
        self.state.reads_fail = True
        self.page.evaluate("() => window.dispatchEvent(new Event('online'))")
        expect(self.page.locator('#operation-status')).to_contain_text('No se pudo actualizar')
        self.assertFalse(self.page.evaluate('AppState.apiOnline'))
        self.state.reads_fail = False
        self.page.evaluate("() => window.dispatchEvent(new Event('online'))")
        self.page.wait_for_function('() => AppState.apiOnline && !AppState.refreshing')
        self.assertFalse(any(r['method'] != 'GET' for r in self.state.requests))

    def test_ios_restore_does_not_resend_pending_operation(self):
        self.prepare()
        self.state.post_fault = {'drop_after': True}
        self.page.locator('#btn-submit-lock').click()
        expect(self.page.locator('#operation-status')).to_contain_text('Alta sin confirmar')
        key = self.page.evaluate('AppState.pendingOperation.key')
        self.page.evaluate("() => window.dispatchEvent(new PageTransitionEvent('pageshow', {persisted:true}))")
        self.page.wait_for_function('() => !AppState.refreshing && AppState.apiOnline')
        self.assertEqual(key, self.page.evaluate('AppState.pendingOperation.key'))
        self.assertEqual(1, sum(r['method'] == 'POST' for r in self.state.requests))
        expect(self.page.locator('#btn-retry-operation')).to_be_visible()

    def test_pwa_manifest_icons_and_offline_deep_link_shell(self):
        self.page.evaluate('() => navigator.serviceWorker.ready')
        self.page.reload(wait_until='networkidle')
        self.page.wait_for_function('() => navigator.serviceWorker.controller !== null')
        manifest = self.page.request.get(self.origin + '/app/manifest.webmanifest').json()
        self.assertEqual('/app/', manifest['scope'])
        for icon in manifest['icons']:
            response = self.page.request.get(self.origin + '/app/' + icon['src'])
            self.assertEqual(200, response.status)
            width, height = struct.unpack('>II', response.body()[16:24])
            self.assertEqual(icon['sizes'], f'{width}x{height}')
        self.context.set_offline(True)
        self.page.goto(self.origin + '/app/?maquina=EQ-ENV-01', wait_until='networkidle')
        expect(self.page.locator('#api-connection-badge')).to_contain_text('estado sin verificar')
        self.assertEqual([], self.page.evaluate('AppState.machines'))
        self.assertEqual('EQ-ENV-01', self.page.evaluate('AppState.pendingMachineLink'))
        keys = self.page.evaluate('''async () => (await Promise.all((await caches.keys()).map(async key =>
          (await (await caches.open(key)).keys()).map(req => ({url:req.url, auth:req.headers.has('Authorization')}))))).flat()''')
        self.assertFalse(any('/api/' in k['url'] or k['auth'] for k in keys))
        self.context.set_offline(False)
        self.page.locator('button[onclick="refreshConnection()"]' ).click()
        expect(self.page.locator('#tech-step-2')).to_be_visible()
        self.assertFalse(any(r['method'] != 'GET' for r in self.state.requests))


if __name__ == '__main__':
    unittest.main()
