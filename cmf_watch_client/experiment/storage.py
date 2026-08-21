"""
Trial storage manager for persisting and loading Experiment 01 trials as versioned JSON documents.
"""

import json
import logging
import os
from typing import List, Optional
from pydantic import ValidationError

from .models import ExperimentTrial

logger = logging.getLogger("cmf_watch_client.experiment.storage")

DEFAULT_TRIALS_DIR = os.path.join("experiments", "experiment_01_distance", "trials")


class TrialStorageManager:
    """Manages filesystem storage, listing, loading, and validation of trial files."""

    def __init__(self, trials_dir: str = DEFAULT_TRIALS_DIR):
        self.trials_dir = trials_dir
        os.makedirs(self.trials_dir, exist_ok=True)

    def get_next_trial_id(self) -> str:
        """Generate next sequential trial ID in storage directory."""
        existing = self.list_trials()
        max_num = 0
        for t in existing:
            tid = t.metadata.trial_id
            if tid.startswith("trial_"):
                try:
                    num = int(tid.split("trial_")[1])
                    if num > max_num:
                        max_num = num
                except ValueError:
                    pass
        return f"trial_{max_num + 1:03d}"

    def save_trial(self, trial: ExperimentTrial, filepath: Optional[str] = None) -> str:
        """
        Save ExperimentTrial model to deterministic JSON file.

        Returns:
            Saved file path.
        """
        if filepath is None:
            filename = f"{trial.metadata.trial_id}.json"
            filepath = os.path.join(self.trials_dir, filename)

        # Serialize Pydantic model with ISO datetime formatting
        json_data = trial.model_dump(mode="json")

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(json_data, f, indent=2, ensure_ascii=False)

        logger.info(f"Successfully saved trial {trial.metadata.trial_id} to '{filepath}'")
        return filepath

    def load_trial(self, filepath: str) -> ExperimentTrial:
        """
        Load and validate ExperimentTrial from JSON file.
        """
        if not os.path.exists(filepath):
            raise FileNotFoundError(f"Trial file not found: '{filepath}'")

        with open(filepath, "r", encoding="utf-8") as f:
            raw_data = json.load(f)

        try:
            return ExperimentTrial.model_validate(raw_data)
        except ValidationError as e:
            logger.error(f"Trial schema validation error loading '{filepath}': {e}")
            raise

    def list_trials(self) -> List[ExperimentTrial]:
        """
        Scan trials directory and return ordered list of valid ExperimentTrial objects.
        """
        trials = []
        if not os.path.exists(self.trials_dir):
            return trials

        for fname in sorted(os.listdir(self.trials_dir)):
            if fname.endswith(".json"):
                fpath = os.path.join(self.trials_dir, fname)
                try:
                    t = self.load_trial(fpath)
                    trials.append(t)
                except Exception as e:
                    logger.warning(f"Skipping invalid trial file '{fname}': {e}")

        return trials
