from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import time
import uuid


def token_estimate(messages):
    text = " ".join(str(message.get("content", "")) for message in messages)
    return max(1, len(text.split()))


def openai_request_from(payload):
    if isinstance(payload.get("request"), dict):
        return payload["request"]
    return payload


class MockLlmHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        if self.path == "/health":
            self.write_json(200, {"status": "UP"})
            return
        self.write_json(404, {"error": {"message": "not found"}})

    def do_POST(self):
        if self.path != "/v1/chat/completions":
            self.write_json(404, {"error": {"message": "not found"}})
            return

        try:
            length = int(self.headers.get("content-length", "0"))
            payload = json.loads(self.rfile.read(length) or "{}")
        except json.JSONDecodeError:
            self.write_json(400, {"error": {"message": "invalid JSON"}})
            return

        openai_request = openai_request_from(payload)
        model = openai_request.get("model") or "mock-gpt-4o-mini"
        messages = openai_request.get("messages")
        if not isinstance(messages, list) or not messages:
            self.write_json(400, {
                "error": {
                    "message": "messages must be a non-empty array",
                    "expected": {
                        "model": "mock-gpt-4o-mini",
                        "messages": [
                            {
                                "role": "user",
                                "content": "Hello"
                            }
                        ]
                    }
                }
            })
            return

        prompt_tokens = token_estimate(messages)
        completion_text = "Mock response from ACIP local LLM."
        completion_tokens = token_estimate([{"content": completion_text}])
        response = {
            "id": f"chatcmpl-mock-{uuid.uuid4().hex[:12]}",
            "object": "chat.completion",
            "created": int(time.time()),
            "model": model,
            "choices": [
                {
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": completion_text,
                    },
                    "finish_reason": "stop",
                }
            ],
            "usage": {
                "prompt_tokens": prompt_tokens,
                "completion_tokens": completion_tokens,
                "total_tokens": prompt_tokens + completion_tokens,
            },
        }
        self.write_json(200, response)

    def log_message(self, format, *args):
        return

    def write_json(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("content-type", "application/json")
        self.send_header("content-length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8080), MockLlmHandler)
    server.serve_forever()
