import requests
import json
import sys
import os
from datetime import datetime
import uuid
import time
import logging
from dotenv import load_dotenv

# Load .env file
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
load_dotenv(os.path.join(SCRIPT_DIR, ".env"))

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Environment variables
CMS_BASE_URL_ORG1 = os.environ.get("CMS_BASE_URL_ORG1")
CMS_BASE_URL_ORG2 = os.environ.get("CMS_BASE_URL_ORG2")
headers = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}

CITY_COORDINATES = {
    "London": (51.5074, -0.1278),
    "Sydney": (-33.8688, 151.2093),
    "New York": (40.7128, -74.0060),
    "Rio": (-22.9068, -43.1729),
    "Tokyo": (35.6762, 139.6503),
    "Cape Town": (-33.9249, 18.4241),
    "Kuala Lumpur": (3.1390, 101.6869),
    "Paris": (48.8566, 2.3522),
    "Mumbai": (19.0760, 72.8777),
    "Toronto": (43.6532, -79.3832)
}

def interpolate_route(start_coords, end_coords, progress):
    """Interpolate coordinates between start and end based on progress (0.0 to 1.0)."""
    lat = start_coords[0] + (end_coords[0] - start_coords[0]) * progress
    lon = start_coords[1] + (end_coords[1] - start_coords[1]) * progress
    return round(lat, 4), round(lon, 4)

def delete_consignment(base_url, consignment_id, auth_token=None):
    url = f"{base_url}/consignments/{consignment_id}"
    local_headers = headers.copy()
    if auth_token:
        local_headers["Authorization"] = f"Bearer {auth_token}"
        logger.debug(f"Setting Authorization header for delete: Bearer {auth_token[:10]}...")
    try:
        response = requests.delete(url, headers=local_headers)
        logger.info(f"DELETE response status: {response.status_code}, body: {response.text}")
        if response.status_code == 204:
            logger.info(f"Deleted consignment: {consignment_id}")
        elif response.status_code == 404:
            logger.info(f"Consignment {consignment_id} not found, no deletion needed")
        else:
            logger.error(f"Failed to delete consignment {consignment_id}: {response.text}")
    except requests.exceptions.RequestException as e:
        logger.error(f"Error deleting consignment {consignment_id}: {e}")
        raise

def create_consignment(base_url, goods, sender_id, receiver_id, tracking_status=None, latitude=None, longitude=None, auth_token=None):
    url = f"{base_url}/consignments"
    payload = {
        "goods": goods,
        "sender": {"id": sender_id},
        "receiver": {"id": receiver_id},
        "organisationId": sender_id,
        "trackingStatus": tracking_status,
        "latitude": latitude,
        "longitude": longitude
    }
    local_headers = headers.copy()
    if auth_token:
        local_headers["Authorization"] = f"Bearer {auth_token}"
        logger.debug(f"Setting Authorization header: Bearer {auth_token[:10]}...")
    logger.debug(f"Payload for consignment request: {json.dumps(payload, indent=2)}")
    try:
        response = requests.post(url, headers=local_headers, data=json.dumps(payload))
        logger.info(f"POST response status: {response.status_code}, body: {response.text}")
        response.raise_for_status()
        logger.info(f"Created consignment: Response: {response.json()}")
        return response.json()
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to create consignment {e.response.text}")
        raise
    except requests.exceptions.RequestException as e:
        logger.error(f"Error creating consignment {payload}: {e}")
        raise

def update_consignment(base_url, consignment_id, goods, sender_id, receiver_id, tracking_status=None, latitude=None, longitude=None, auth_token=None):
    url = f"{base_url}/consignments/{consignment_id}"
    payload = {
        "id": consignment_id,
        "goods": goods,
        "sender": {"id": sender_id},
        "receiver": {"id": receiver_id},
        "organisationId": sender_id,
        "trackingStatus": tracking_status,
        "latitude": latitude,
        "longitude": longitude
    }
    local_headers = headers.copy()
    if auth_token:
        local_headers["Authorization"] = f"Bearer {auth_token}"
        logger.debug(f"Setting Authorization header for update: Bearer {auth_token[:10]}...")
    logger.info(f"Payload for update request: {payload}")
    try:
        response = requests.put(url, headers=local_headers, data=json.dumps(payload))
        logger.info(f"PUT response status: {response.status_code}, body: {response.text}")
        response.raise_for_status()
        logger.info(f"Updated consignment: {consignment_id}, Response: {response.json()}")
        return response.json()
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to update consignment {consignment_id}: {e.response.text}")
        raise
    except requests.exceptions.RequestException as e:
        logger.error(f"Error updating consignment {consignment_id}: {e}")
        raise

def get_consignment(base_url, consignment_id, auth_token=None):
    url = f"{base_url}/consignments/{consignment_id}"
    local_headers = headers.copy()
    if auth_token:
        local_headers["Authorization"] = f"Bearer {auth_token}"
        logger.debug(f"Setting Authorization header for get: Bearer {auth_token[:10]}...")
    try:
        response = requests.get(url, headers=local_headers)
        logger.info(f"GET response status: {response.status_code}, body: {response.text}")
        if response.status_code == 404:
            logger.info(f"Consignment {consignment_id} not found in database")
            return None
        response.raise_for_status()
        return response.json()
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to get consignment {consignment_id}: {e.response.text}")
        raise
    except requests.exceptions.RequestException as e:
        logger.error(f"Error getting consignment {consignment_id}: {e}")
        raise

def get_organisation(base_url, org_id, auth_token=None):
    url = f"{base_url}/organisations/{org_id}"
    local_headers = headers.copy()
    if auth_token:
        local_headers["Authorization"] = f"Bearer {auth_token}"
        logger.debug(f"Setting Authorization header for get organisation: Bearer {auth_token[:10]}...")
    try:
        response = requests.get(url, headers=local_headers)
        logger.info(f"GET organisation response status: {response.status_code}, body: {response.text}")
        response.raise_for_status()
        return response.json()
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to get organisation {org_id}: {e.response.text}")
        raise
    except requests.exceptions.RequestException as e:
        logger.error(f"Error getting organisation {org_id}: {e}")
        raise

def poll_consignment_state(base_url, consignment_id, auth_token, org_name, org_id, expected_status, expected_lat=None, expected_lon=None, expected_ver=None, timeout=180, interval=10):
    """Poll for consignment state until expected values are met or timeout is reached."""
    logger.info(f"Polling for consignment state for {org_name}, {org_id} up to {timeout}s...")
    start_time = time.time()
    last_state = None
    while time.time() - start_time < timeout:
        try:
            state = get_consignment(base_url, consignment_id, auth_token)
            last_state = state
            if state:
                logger.info(f"status: {state.get('trackingStatus')}, lat: {state.get('latitude')}, lon: {state.get('longitude')}, ver: {state.get('ver', 'unknown')}")
                if (state.get("trackingStatus") == expected_status and
                        (expected_lat is None or state.get("latitude") == expected_lat) and
                        (expected_lon is None or state.get("longitude") == expected_lon) and
                        (expected_ver is None or state.get("ver") == expected_ver)):
                    logger.info(f"Consignment state confirmed for {org_name}, {org_id}: {state}")
                    return state
            logger.info(f"State not detected for {org_name}, {org_id}. Retrying...")
            time.sleep(interval)
        except requests.exceptions.RequestException as e:
            logger.error(f"API error for {org_name}, {org_id}: {e}. Retrying...")
            time.sleep(interval)
    logger.error(f"Failed to detect consignment state for {org_name}, {org_id} in {timeout}s. Last state: {last_state}")
    sys.exit(1)

def main():
    try:
        # Validate required environment variables
        required_env_vars = ["CMS_AUTH_TOKEN_ORG1", "ORG1_ID", "ORG1_NAME", "CMS_AUTH_TOKEN_ORG2", "ORG2_ID", "ORG2_NAME", "CMS_BASE_URL_ORG1", "CMS_BASE_URL_ORG2"]
        for var in required_env_vars:
            if not os.environ.get(var):
                logger.error(f"Error: Environment variable {var} is not set")
                sys.exit(1)

        # Select auth tokens and Organisation details
        cms_auth_token_org1 = os.environ.get("CMS_AUTH_TOKEN_ORG1")
        cms_auth_token_org2 = os.environ.get("CMS_AUTH_TOKEN_ORG2")
        org1_id = os.environ.get("ORG1_ID")
        org2_id = os.environ.get("ORG2_ID")
        org1_name = os.environ.get("ORG1_NAME")
        org2_name = os.environ.get("ORG2_NAME")
        logger.info(f"Using CMS_AUTH_TOKEN_ORG1: {cms_auth_token_org1[:10]}...{cms_auth_token_org1[-10:]}, CMS_AUTH_TOKEN_ORG2: {cms_auth_token_org2[:10]}...{cms_auth_token_org2[-10:]}, ORG1_ID: {org1_id}, ORG1_NAME: {org1_name}, ORG2_ID: {org2_id}, ORG2_NAME: {org2_name}")

        # Fetch Organisation details for Org1 and Org2
        logger.info(f"User 1: Fetching organisation details for {org1_id} at {datetime.now()}")
        org1 = get_organisation(CMS_BASE_URL_ORG1, org1_id, cms_auth_token_org1)
        logger.info(f"User 2: Fetching organisation details for {org2_id} at {datetime.now()}")
        org2 = get_organisation(CMS_BASE_URL_ORG2, org2_id, cms_auth_token_org2)
        org1_city = org1.get("city", "London")
        org2_city = org2.get("city", "Sydney")
        start_coords = CITY_COORDINATES.get(org1_city, CITY_COORDINATES["London"])
        end_coords = CITY_COORDINATES.get(org2_city, CITY_COORDINATES["Sydney"])
        logger.info(f"Route from {org1_city} {start_coords} to {org2_city} {end_coords}")

        # Create consignment by Org1
        logger.info(f"User 1: Creating consignment for {org1_id} at {datetime.now()}")
        cons1 = create_consignment(
            base_url=CMS_BASE_URL_ORG1,
            goods={"item1": 10, "item2": 20},
            sender_id=org1_id,
            receiver_id=org2_id,
            tracking_status="CREATED",
            latitude=start_coords[0],
            longitude=start_coords[1],
            auth_token=cms_auth_token_org1
        )
        logger.info(f"Created consignment: cons1={cons1['idControl']}")

        # Wait for blockchain submission
        logger.info(f"User 1: Waiting 60 seconds for blockchain submission for {org1_name}, {org1_id}...")
        time.sleep(60)

        # Org2 polls for initial consignment state
        poll_consignment_state(
            base_url=CMS_BASE_URL_ORG2,
            consignment_id=cons1['idControl'],
            auth_token=cms_auth_token_org2,
            org_name=org2_name,
            org_id=org2_id,
            expected_status="CREATED",
            expected_ver=1
        )

        # Update consignment by Org1
        logger.info(f"User 1: Updating consignment with tracking status and location for {org1_name}, {org1_id} at {datetime.now()}")
        cons1_lat, cons1_lon = interpolate_route(start_coords, end_coords, 0.5)
        update_consignment(
            base_url=CMS_BASE_URL_ORG1,
            consignment_id=cons1['idControl'],
            goods={"item1": 10, "item2": 20},
            sender_id=org1_id,
            receiver_id=org2_id,
            tracking_status="IN_TRANSIT",
            latitude=cons1_lat,
            longitude=cons1_lon,
            auth_token=cms_auth_token_org1
        )

        # Org2 polls for updated consignment state
        poll_consignment_state(
            base_url=CMS_BASE_URL_ORG2,
            consignment_id=cons1['idControl'],
            auth_token=cms_auth_token_org2,
            org_name=org2_name,
            org_id=org2_id,
            expected_status="IN_TRANSIT",
            expected_lat=cons1_lat,
            expected_lon=cons1_lon,
            expected_ver=2
        )

        # Org1 polls for final consignment state
        poll_consignment_state(
            base_url=CMS_BASE_URL_ORG1,
            consignment_id=cons1['idControl'],
            auth_token=cms_auth_token_org1,
            org_name=org1_name,
            org_id=org1_id,
            expected_status="IN_TRANSIT",
            expected_ver=2
        )

        logger.info(f"Test completed successfully for {org1_name}, {org1_id} and {org2_name}, {org2_id}")

    except Exception as e:
        logger.error(f"Test failed for {org1_name}, {org1_id} and {org2_name}, {org2_id}: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()