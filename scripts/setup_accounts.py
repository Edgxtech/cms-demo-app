import requests
import json
import sys
import yaml
import jwt
import os
import uuid
from datetime import datetime
from bip_utils import Bip39MnemonicGenerator, Bip39WordsNum
import argparse
import re
import random
import time
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Keycloak configuration
KEYCLOAK_URL = os.environ.get("KEYCLOAK_URL", "http://keycloak:8080")
ADMIN_USERNAME = os.environ.get("KEYCLOAK_ADMIN_USERNAME", "admin")
ADMIN_PASSWORD = os.environ.get("KEYCLOAK_ADMIN_PASSWORD", "admin")
REALM_NAME = "cms-demo-realm"
CLIENT_ID = os.environ.get("KEYCLOAK_CLIENT_ID", "cms-demo-app")
ORG_NAME = os.environ.get("ORG_NAME")
CMS_APP_PORT = os.environ.get("CMS_APP_PORT")

# Dummy city coordinates for Organisation creation
CITY_COORDINATES = {
    "New York": {"lat": 40.7128, "lon": -74.0060},
    "Los Angeles": {"lat": 34.0522, "lon": -118.2437},
    "Chicago": {"lat": 41.8781, "lon": -87.6298}
}

# Dynamic path resolution
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = "/app"
SCRIPTS_DOT_ENV_FILE = os.path.join(SCRIPT_DIR, ".env")

def parse_arguments():
    parser = argparse.ArgumentParser(description="Keycloak and application setup script")
    parser.add_argument(
        "--generate-mnemonic",
        action="store_true",
        help="Generate Cardano mnemonic required as owners wallet account to pay for transactions"
    )
    return parser.parse_args()

def check_app_health(max_attempts=30, delay=5):
    """Check if CMS app is healthy by calling the /api/hello endpoint"""
    # Use Docker service name 'app' instead of localhost for in-container health check
    health_url = f"http://app:{CMS_APP_PORT}/api/hello"
    logger.info(f"Trying: {health_url}")
    headers = {"Content-Type": "application/json"}

    for attempt in range(max_attempts):
        try:
            response = requests.get(health_url, headers=headers, timeout=5)
            if response.status_code == 200 and "Hello" in response.text:
                logger.info("CMS app is healthy")
                return True
            logger.info(f"Health check attempt {attempt + 1}/{max_attempts}: App not ready yet")
        except requests.exceptions.RequestException as e:
            logger.info(f"Health check attempt {attempt + 1}/{max_attempts}: {e}")

        time.sleep(delay)

    logger.error("CMS app failed to become healthy after maximum attempts")
    sys.exit(1)

def generate_cardano_mnemonic():
    try:
        mnemonic = Bip39MnemonicGenerator().FromWordsNumber(Bip39WordsNum.WORDS_NUM_24)
        return str(mnemonic)
    except Exception as e:
        logger.error(f"Failed to generate Cardano mnemonic: {e}")
        sys.exit(1)

def get_admin_token():
    url = f"{KEYCLOAK_URL}/realms/master/protocol/openid-connect/token"
    payload = {
        "grant_type": "password",
        "client_id": "admin-cli",
        "username": ADMIN_USERNAME,
        "password": ADMIN_PASSWORD
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    try:
        response = requests.post(url, data=payload, headers=headers)
        response.raise_for_status()
        return response.json()["access_token"]
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to get admin token: {e.response.text}")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        logger.error(f"Error getting admin token: {e}")
        sys.exit(1)

def create_realm(admin_token):
    url = f"{KEYCLOAK_URL}/admin/realms"
    payload = {
        "realm": REALM_NAME,
        "enabled": True
    }
    headers = {
        "Authorization": f"Bearer {admin_token}",
        "Content-Type": "application/json"
    }
    try:
        response = requests.post(url, json=payload, headers=headers)
        if response.status_code == 201:
            logger.info(f"Created realm: {REALM_NAME}")
        elif response.status_code == 409:
            logger.info(f"Realm {REALM_NAME} already exists")
        else:
            logger.error(f"Failed to create realm: {response.status_code} {response.text}")
            sys.exit(1)
    except requests.exceptions.RequestException as e:
        logger.error(f"Error creating realm: {e}")
        sys.exit(1)

def delete_client(admin_token, client_id):
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM_NAME}/clients/{client_id}"
    headers = {
        "Authorization": f"Bearer {admin_token}",
        "Content-Type": "application/json"
    }
    try:
        response = requests.delete(url, headers=headers)
        if response.status_code == 204:
            logger.info(f"Deleted existing client: {CLIENT_ID}")
        elif response.status_code == 404:
            logger.info(f"Client {CLIENT_ID} not found, no deletion needed")
        else:
            logger.error(f"Failed to delete client: {response.status_code} {response.text}")
    except requests.exceptions.RequestException as e:
        logger.error(f"Error deleting client: {e}")

def create_client(admin_token):
    client_id = get_client_id(admin_token)
    if client_id:
        logger.info(f"Client {CLIENT_ID} exists, updating configuration")
        update_client(admin_token, client_id)
    else:
        create_new_client(admin_token)
    client_id = get_client_id(admin_token)
    secret_url = f"{KEYCLOAK_URL}/admin/realms/{REALM_NAME}/clients/{client_id}/client-secret"
    headers = {
        "Authorization": f"Bearer {admin_token}",
        "Content-Type": "application/json"
    }
    try:
        response = requests.get(secret_url, headers=headers)
        response.raise_for_status()
        client_secret = response.json()["value"]
        logger.info(f"Retrieved client secret: {client_secret[:8]}...")
        return client_secret
    except requests.exceptions.RequestException as e:
        logger.error(f"Error retrieving client secret: {e}")
        sys.exit(1)

def create_new_client(admin_token):
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM_NAME}/clients"
    payload = {
        "clientId": CLIENT_ID,
        "enabled": True,
        "protocol": "openid-connect",
        "clientAuthenticatorType": "client-secret",
        "serviceAccountsEnabled": True,
        "publicClient": False,
        "directAccessGrantsEnabled": True,
        "defaultClientScopes": ["profile", "email"],
        "attributes": {
            "access.token.lifespan": "604800"
        },
        "protocolMappers": [
            {
                "name": "organisations-mapper",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-attribute-mapper",
                "consentRequired": False,
                "config": {
                    "user.attribute": "organisations",
                    "claim.name": "organisations",
                    "jsonType.label": "String",
                    "multivalued": "true",
                    "aggregate.attrs": "false",
                    "access.token.claim": "true",
                    "id.token.claim": "false",
                    "userInfo.token.claim": "false"
                }
            },
            {
                "name": "name-mapper",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-property-mapper",
                "consentRequired": False,
                "config": {
                    "user.attribute": "username",
                    "claim.name": "name",
                    "jsonType.label": "String",
                    "multivalued": "false",
                    "access.token.claim": "true",
                    "id.token.claim": "false",
                    "userinfo.token.claim": "true"
                }
            }
        ]
    }
    headers = {
        "Authorization": f"Bearer {admin_token}",
        "Content-Type": "application/json"
    }
    try:
        response = requests.post(url, json=payload, headers=headers)
        if response.status_code == 201:
            logger.info(f"Created client: {CLIENT_ID}")
        else:
            logger.error(f"Failed to create client: {response.status_code} {response.text}")
            sys.exit(1)
    except requests.exceptions.RequestException as e:
        logger.error(f"Error creating client: {e}")
        sys.exit(1)

def update_client(admin_token, client_id):
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM_NAME}/clients/{client_id}"
    payload = {
        "clientId": CLIENT_ID,
        "enabled": True,
        "protocol": "openid-connect",
        "clientAuthenticatorType": "client-secret",
        "serviceAccountsEnabled": True,
        "publicClient": False,
        "directAccessGrantsEnabled": True,
        "defaultClientScopes": ["profile", "email"],
        "attributes": {
            "access.token.lifespan": "604800"
        }
    }
    headers = {
        "Authorization": f"Bearer {admin_token}",
        "Content-Type": "application/json"
    }
    try:
        response = requests.put(url, json=payload, headers=headers)
        response.raise_for_status()
        logger.info(f"Updated client {CLIENT_ID}")
    except requests.exceptions.RequestException as e:
        logger.error(f"Error updating client: {e}")
        sys.exit(1)

def get_client_id(admin_token):
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM_NAME}/clients?clientId={CLIENT_ID}"
    headers = {
        "Authorization": f"Bearer {admin_token}",
        "Content-Type": "application/json"
    }
    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        clients = response.json()
        if clients:
            return clients[0]["id"]
        return None
    except requests.exceptions.RequestException as e:
        logger.error(f"Error getting client ID: {e}")
        return None

def assign_organisation_to_service_account(admin_token, client_id, organisation_id):
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM_NAME}/clients/{client_id}/service-account-user"
    headers = {
        "Authorization": f"Bearer {admin_token}",
        "Content-Type": "application/json"
    }
    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        service_account_user = response.json()
        user_id = service_account_user["id"]
        url = f"{KEYCLOAK_URL}/admin/realms/{REALM_NAME}/users/{user_id}"
        payload = {
            "attributes": {
                "organisations": [organisation_id]
            }
        }
        response = requests.put(url, json=payload, headers=headers)
        response.raise_for_status()
        logger.info(f"Assigned organisation {organisation_id} to service account for client {CLIENT_ID}")
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to assign organisation to service account: {e.response.text}")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        logger.error(f"Error assigning organisation to service account: {e}")
        sys.exit(1)

def get_access_token(client_secret):
    url = f"{KEYCLOAK_URL}/realms/{REALM_NAME}/protocol/openid-connect/token"
    payload = {
        "grant_type": "client_credentials",
        "client_id": CLIENT_ID,
        "client_secret": client_secret,
        "scope": "openid profile email"
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    try:
        response = requests.post(url, data=payload, headers=headers)
        response.raise_for_status()
        token = response.json()["access_token"]
        decoded = jwt.decode(token, options={"verify_signature": False})
        logger.info(f"Token claims: {json.dumps(decoded, indent=2)}")
        logger.info(f"Obtained access token: {token[:10]}...")
        organisations = decoded.get("organisations")
        name = decoded.get("name")
        if not organisations or not isinstance(organisations, list):
            logger.error("Error: 'organisations' claim missing or not a list in token")
            sys.exit(1)
        if not name:
            logger.error("Error: 'name' claim missing in token")
            sys.exit(1)
        return token
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to get access token: {e.response.text}")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        logger.error(f"Error getting access token: {e}")
        sys.exit(1)

def create_organisation(name, currency_id, tax_id_number, organisation_id=None, cms_auth_token=None):
    url = f"http://app:{CMS_APP_PORT}/api/organisations"
    headers = {"Content-Type": "application/json"}
    if cms_auth_token:
        headers["Authorization"] = f"Bearer {cms_auth_token}"
        logger.debug(f"Setting Authorization header: Bearer {cms_auth_token[:10]}...")
    cities = list(CITY_COORDINATES.keys())
    city = random.choice(cities)
    payload = {
        "id": organisation_id,
        "name": name,
        "city": city,
        "postCode": "12345",
        "province": "CA",
        "countryCode": "US",
        "address": "123 Main St",
        "phoneNumber": "123-456-7890",
        "taxIdNumber": tax_id_number,
        "dummyAccount": None,
        "preApproveTransactions": False,
        "preApproveTransactionsDispatch": False,
        "accountPeriodDays": 30,
        "currencyId": currency_id,
        "reportCurrencyId": currency_id,
        "websiteUrl": None,
        "adminEmail": f"admin_{name.lower()}@example.com",
        "logo": None
    }
    try:
        if organisation_id:
            check_url = f"http://app:{CMS_APP_PORT}/api/organisations/{organisation_id}"
            check_response = requests.get(check_url, headers=headers)
            logger.info(f"GET response status: {check_response.status_code}, body: {check_response.text}")
            if check_response.status_code == 200:
                logger.info(f"Organisation {organisation_id} already exists, skipping creation")
                return check_response.json()
            elif check_response.status_code in (401, 404):
                logger.info(f"Organisation {organisation_id} not found or unauthorized, attempting creation")
        response = requests.post(url, headers=headers, data=json.dumps(payload))
        logger.info(f"POST response status: {response.status_code}, body: {response.text}")
        response.raise_for_status()
        logger.info(f"Created organisation: {name} (ID: {organisation_id or 'auto-generated'}), Response: {response.json()}")
        return response.json()
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to process organisation {name} (ID: {organisation_id or 'N/A'}): {e.response.text}")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        logger.error(f"Error processing organisation {name} (ID: {organisation_id or 'N/A'}): {e}")
        sys.exit(1)

def write_env_vars(cms_auth_token, org_id):
    # write variables required for demo scripts when run from host
    try:
        org_upper = ORG_NAME.upper()
        new_vars = {
            f"{org_upper}_ID": org_id,
            f"{org_upper}_NAME": ORG_NAME,
            f"CMS_AUTH_TOKEN_{org_upper}": cms_auth_token,
            f"CMS_BASE_URL_{org_upper}": f'"http://localhost:{CMS_APP_PORT}/api"'
        }

        lines = []
        if os.path.exists(SCRIPTS_DOT_ENV_FILE):
            with open(SCRIPTS_DOT_ENV_FILE, 'r') as f:
                lines = f.readlines()

        existing_vars = {}
        line_indices = {}
        for i, line in enumerate(lines):
            stripped_line = line.strip()
            if stripped_line and not stripped_line.startswith('#') and '=' in stripped_line:
                key, value = stripped_line.split("=", 1)
                key = key.strip()
                existing_vars[key] = value.strip()
                line_indices[key] = i

        existing_vars.update(new_vars)
        updated_lines = lines.copy() if lines else []
        for key, value in new_vars.items():
            if key in line_indices:
                original_line = lines[line_indices[key]]
                indent = original_line[:original_line.index(key)]
                updated_lines[line_indices[key]] = f"{indent}{key}={value}\n"
            else:
                updated_lines.append(f"{key}={value}\n")

        temp_file = f"{SCRIPTS_DOT_ENV_FILE}.tmp"
        with open(temp_file, 'w') as f:
            f.writelines(updated_lines)
        os.replace(temp_file, SCRIPTS_DOT_ENV_FILE)
        logger.info(f"Wrote/updated environment variables to {SCRIPTS_DOT_ENV_FILE}")
        os.chmod(SCRIPTS_DOT_ENV_FILE, 0o600)
    except Exception as e:
        logger.error(f"Failed to write {SCRIPTS_DOT_ENV_FILE}: {e}")
        sys.exit(1)

def main():
    try:
        args = parse_arguments()
        logger.info(f"Setting up Keycloak and CMS for {ORG_NAME} at {datetime.now()}")

        # Validate required environment variables
        required_env_vars = ["KEYCLOAK_URL", "KEYCLOAK_CLIENT_ID", "ORG_NAME", "CMS_BASE_URL"]
        for var in required_env_vars:
            if not os.environ.get(var):
                logger.error(f"Error: Environment variable {var} is not set")
                sys.exit(1)

        # Check CMS app health
        logger.info("Checking CMS app health")
        check_app_health()

        # Generate Organisation ID
        logger.info(f"Generating Organisation ID for {ORG_NAME}")
        org_id = str(uuid.uuid4())
        logger.info(f"Generated Organisation ID - {ORG_NAME}: {org_id}")

        # Generate Cardano mnemonic if requested
        if args.generate_mnemonic:
            logger.info("Generating Cardano mnemonic")
            mnemonic = generate_cardano_mnemonic()
            org_file = f"{ORG_NAME}.env"
            logger.info(f"Generated Cardano Mnemonic: {mnemonic}")
            logger.info(f"Please manually update {org_file} with the following:")
            logger.info(f'LOB_OWNER_ACCOUNT_MNEMONIC="{mnemonic}"')
        else:
            logger.info("Skipping Cardano mnemonic generation (use --generate-mnemonic to enable)")

        # Set up Keycloak
        admin_token = get_admin_token()
        create_realm(admin_token)
        client_secret = create_client(admin_token)
        client_id = get_client_id(admin_token)
        assign_organisation_to_service_account(admin_token, client_id, org_id)
        access_token = get_access_token(client_secret)
        logger.info("Keycloak setup is Ok!")

        # Create organisation in CMS
        logger.info(f"Creating organisation in CMS: {ORG_NAME}")
        currency_id = "USD"  # Default currency, could be made configurable
        tax_id_number = f"TAX-{str(uuid.uuid4())[:8]}"  # Generate unique tax ID
        org_response = create_organisation(ORG_NAME, currency_id, tax_id_number, org_id, access_token)
        logger.info(f"CMS app setup is Ok!")

        # Write environment variables
        write_env_vars(access_token, org_id)

        logger.info(f"Setup completed for {ORG_NAME}")
        return access_token, org_id
    except Exception as e:
        logger.error(f"Setup failed for {ORG_NAME}: {e}")
        sys.exit(1)

if __name__ == "__main__":
    access_token, org_id = main()
    logger.info(f"\nSetup completed successfully for {ORG_NAME}")
    logger.info(f"Access token: {access_token[:10]}...")
    logger.info(f"Organisation ID: {org_id}")