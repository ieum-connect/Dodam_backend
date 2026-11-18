"""Local HTTP adapter for the existing Dodam AI vaccination engine."""

import hmac
import json
import os
from datetime import date
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from app.domain.vaccination.engine import compute_schedule
from app.domain.vaccination.error import VaccinationError
from app.domain.vaccination.schedule_data import load_schedule


class VaccinationHandler(BaseHTTPRequestHandler):
    def send_json(self, status, body):
        payload = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self):
        if self.path == "/health":
            self.send_json(200, {"status": "ok"})
        else:
            self.send_json(404, {})

    def read_body(self):
        limit = 16_384
        if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
            body = bytearray()
            while True:
                size = int(self.rfile.readline(128).split(b";", 1)[0].strip(), 16)
                if size == 0:
                    return bytes(body)
                if size < 0 or len(body) + size > limit:
                    raise ValueError
                body.extend(self.rfile.read(size))
                if self.rfile.read(2) != b"\r\n":
                    raise ValueError
        length = int(self.headers.get("Content-Length", "0"))
        if not 0 < length <= limit:
            raise ValueError
        return self.rfile.read(length)

    def do_POST(self):
        if self.path != "/internal/vaccinations/schedule":
            self.send_json(404, {})
            return
        supplied = self.headers.get("X-Internal-Token", "").encode()
        if not hmac.compare_digest(supplied, os.environ["AI_INTERNAL_TOKEN"].encode()):
            self.send_json(401, {})
            return
        try:
            payload = json.loads(self.read_body())
            result = compute_schedule(
                load_schedule(), date.fromisoformat(payload["birthDate"]),
                date.fromisoformat(payload["asOf"]), payload["completedDoses"],
            )
        except VaccinationError as exc:
            self.send_json(exc.status_code, {
                "code": exc.code.value,
                "message": exc.message,
                "unknownDoseIds": exc.unknown_dose_ids or [],
            })
        except (KeyError, TypeError, ValueError):
            self.send_json(400, {})
        else:
            self.send_json(200, result)


if __name__ == "__main__":
    load_schedule()
    address = ("127.0.0.1", int(os.environ["DODAM_SHOWCASE_VACCINATION_PORT"]))
    ThreadingHTTPServer(address, VaccinationHandler).serve_forever()
