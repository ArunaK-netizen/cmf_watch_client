from setuptools import setup, find_packages

setup(
    name="cmf-watch-client",
    version="1.0.0",
    packages=find_packages(),
    install_requires=[
        "bleak>=0.20.0",
        "cryptography>=38.0.0",
        "pydantic>=2.0.0",
    ],
    entry_points={
        "console_scripts": [
            "cmf-watch-client=cmf_watch_client.cli.main:main",
        ],
    },
)
