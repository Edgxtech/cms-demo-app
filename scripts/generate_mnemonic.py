#!/usr/bin/env python3

import logging
from bip_utils import Bip39MnemonicGenerator, Bip39WordsNum
import os
import argparse

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def parse_arguments():
    parser = argparse.ArgumentParser(description="Generate Cardano mnemonic for wallet account")
    parser.add_argument(
        "--org-name",
        default=os.environ.get("ORG_NAME"),
        help="Organization name for the mnemonic (default: ORG_NAME env variable)"
    )
    return parser.parse_args()

def generate_cardano_mnemonic():
    try:
        mnemonic = Bip39MnemonicGenerator().FromWordsNumber(Bip39WordsNum.WORDS_NUM_24)
        return str(mnemonic)
    except Exception as e:
        logger.error(f"Failed to generate Cardano mnemonic: {e}")
        raise

def main():
    try:
        args = parse_arguments()
        org_name = args.org_name
        if not org_name:
            logger.error("Error: Organization name must be provided via --org-name or ORG_NAME environment variable")
            return 1

        logger.info(f"Generating Cardano mnemonic for {org_name}")
        mnemonic = generate_cardano_mnemonic()

        org_file = f"{org_name}.env"
        logger.info(f"Generated Cardano Mnemonic: {mnemonic}")
        logger.info(f"Please manually update {org_file} with the following:")
        logger.info(f'LOB_OWNER_ACCOUNT_MNEMONIC="{mnemonic}"')

        return 0
    except Exception as e:
        logger.error(f"Mnemonic generation failed: {e}")
        return 1

if __name__ == "__main__":
    exit(main())