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
CMS_BASE_URL = os.environ.get("CMS_BASE_URL_ORG1")
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
    start_lat, start_lon = start_coords
    end_lat, end_lon = end_coords
    lat = start_lat + (end_lat - start_lat) * progress
    lon = start_lon + (end_lon - start_lon) * progress
    return lat, lon

def delete_consignment(consignment_id, cms_auth_token=None):
    url = f"{CMS_BASE_URL}/consignments/{consignment_id}"
    local_headers = headers.copy()
    if cms_auth_token:
        local_headers["Authorization"] = f"Bearer {cms_auth_token}"
        logger.debug(f"Setting Authorization header for delete: Bearer {cms_auth_token[:10]}...")
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

def create_consignment(goods, sender_id, receiver_id, tracking_status=None, latitude=None, longitude=None, cms_auth_token=None):
    url = f"{CMS_BASE_URL}/consignments"
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
    if cms_auth_token:
        local_headers["Authorization"] = f"Bearer {cms_auth_token}"
        logger.debug(f"Setting Authorization header: Bearer {cms_auth_token[:10]}...")
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

def update_consignment(consignment_id, goods, sender_id, receiver_id, tracking_status=None, latitude=None, longitude=None, cms_auth_token=None):
    url = f"{CMS_BASE_URL}/consignments/{consignment_id}"
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
    if cms_auth_token:
        local_headers["Authorization"] = f"Bearer {cms_auth_token}"
        logger.debug(f"Setting Authorization header for update: Bearer {cms_auth_token[:10]}...")
    logger.info(f"Payload for update request: {json.dumps(payload, indent=2)}")
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

def get_consignment(consignment_id, cms_auth_token=None):
    url = f"{CMS_BASE_URL}/consignments/{consignment_id}"
    local_headers = headers.copy()
    if cms_auth_token:
        local_headers["Authorization"] = f"Bearer {cms_auth_token}"
        logger.debug(f"Setting Authorization header for get: Bearer {cms_auth_token[:10]}...")
    try:
        response = requests.get(url, headers=local_headers)
        logger.info(f"GET response status: {response.status_code}, body: {response.text}")
        response.raise_for_status()
        return response.json()
    except requests.exceptions.HTTPError as e:
        logger.error(f"Failed to get consignment {consignment_id}: {e.response.text}")
        raise
    except requests.exceptions.RequestException as e:
        logger.error(f"Error getting consignment {consignment_id}: {e}")
        raise

def get_organisation(org_id, cms_auth_token=None):
    url = f"{CMS_BASE_URL}/organisations/{org_id}"
    local_headers = headers.copy()
    if cms_auth_token:
        local_headers["Authorization"] = f"Bearer {cms_auth_token}"
        logger.debug(f"Setting Authorization header for get organisation: Bearer {cms_auth_token[:10]}...")
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

def main():
    try:
        # Validate required environment variables
        required_env_vars = ["CMS_AUTH_TOKEN_ORG1", "ORG1_ID", "ORG1_NAME", "CMS_BASE_URL_ORG1"]
        for var in required_env_vars:
            if not os.environ.get(var):
                logger.error(f"Error: Environment variable {var} is not set")
                sys.exit(1)

        # Select the auth token and Organisation details for Org1
        cms_auth_token_org1 = os.environ.get("CMS_AUTH_TOKEN_ORG1")
        org1_id = os.environ.get("ORG1_ID")
        org1_name = os.environ.get("ORG1_NAME")
        # Generate a temporary Org2 ID for the receiver
        org2_id = str(uuid.uuid4())
        org2_name = "org2"
        logger.info(f"Using CMS_AUTH_TOKEN_ORG1: {cms_auth_token_org1[:10]}...{cms_auth_token_org1[-10:]}, ORG1_ID: {org1_id}, ORG1_NAME: {org1_name}, ORG2_ID: {org2_id}, ORG2_NAME: {org2_name}")

        # Fetch Org1 details to get the city
        logger.info(f"Fetching organisation details for {org1_id} at {datetime.now()}")
        org1 = get_organisation(org1_id, cms_auth_token_org1)
        org1_city = org1.get("city", "London")  # Fallback to London if city not found
        org2_city = "Sydney"  # Default for receiver
        start_coords = CITY_COORDINATES.get(org1_city, CITY_COORDINATES["London"])
        end_coords = CITY_COORDINATES.get(org2_city, CITY_COORDINATES["Sydney"])
        logger.info(f"Route from {org1_city} {start_coords} to {org2_city} {end_coords}")

        # Create one consignment
        logger.info(f"Creating consignment for {org1_id} at {datetime.now()}")
        cons = create_consignment(
            goods={"item1": 10, "item2": 20},
            sender_id=org1_id,
            receiver_id=org2_id,
            tracking_status="CREATED",
            latitude=start_coords[0],
            longitude=start_coords[1],
            cms_auth_token=cms_auth_token_org1
        )
        logger.info(f"Created consignment: cons={cons['idControl']}")

        # Wait for blockchain submission
        logger.info(f"Waiting 45 seconds for blockchain submission for {org1_name}, {org1_id}...")
        time.sleep(45)

        # Update consignment
        logger.info(f"Updating consignment with tracking status and location for {org1_name}, {org1_id} at {datetime.now()}")
        cons_lat, cons_lon = interpolate_route(start_coords, end_coords, 0.5)
        update_consignment(
            consignment_id=cons['idControl'],
            goods={"item1": 10, "item2": 20},
            sender_id=org1_id,
            receiver_id=org2_id,
            tracking_status="IN_TRANSIT",
            latitude=cons_lat,
            longitude=cons_lon,
            cms_auth_token=cms_auth_token_org1
        )

        # Verify database state
        logger.info(f"Verifying database state for {org1_id} at {datetime.now()}")
        cons_state = get_consignment(cons['idControl'], cms_auth_token=cms_auth_token_org1)

        # Poll for consignment update
        logger.info(f"Polling for update for {org1_name}, {org1_id} up to 180s...")
        start_time = time.time()
        while time.time() - start_time < 180:
            try:
                cons_state = get_consignment(cons['idControl'], cms_auth_token=cms_auth_token_org1)
                logger.info(f"status: {cons_state.get("trackingStatus")}, lat: {cons_state.get("latitude")}, lon: {cons_state.get("longitude")}")
                if (cons_state.get("trackingStatus") == "IN_TRANSIT" and
                        cons_state.get("latitude") == cons_lat and
                        cons_state.get("longitude") == cons_lon):
                    logger.info(f"Consignment updated for {org1_name}, {org1_id}")
                    break
                logger.info(f"Update not detected. Retrying...")
                time.sleep(10)
            except requests.exceptions.RequestException as e:
                logger.error(f"API error: {e}. Retrying...")
                time.sleep(10)
        else:
            logger.error(f"Failed to detect update for {org1_name}, {org1_id} in 180s. Last state: {cons_state}")
            sys.exit(1)

        logger.info(f"Test completed successfully for {org1_name}, {org1_id}")

    except Exception as e:
        logger.error(f"Scenario failed for {org1_name}, {org1_id}: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
