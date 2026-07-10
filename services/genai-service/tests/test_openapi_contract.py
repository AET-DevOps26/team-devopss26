"""Validates that the running app's actual responses conform to api/genai-service.yaml,
the same guarantee the Spring services get for free from openApi().isValid(...) in their
controller tests.
"""

from pathlib import Path

from hypothesis import HealthCheck, settings
import schemathesis
from schemathesis.specs.openapi.checks import ignored_auth

SPEC_PATH = Path(__file__).parent.parent.parent.parent / "api" / "genai-service.yaml"
schema = schemathesis.openapi.from_path(str(SPEC_PATH))


@schema.parametrize()
@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
def test_matches_openapi_spec(case, app, auth_headers):
    # Routes are mounted under /api/genai (mirroring the Spring services'
    # server.servlet.context-path), which the spec itself doesn't encode.
    response = case.call_asgi(app=app, base_url="http://test/api/genai", headers=auth_headers)
    case.validate_response(response, excluded_checks=(ignored_auth,))
