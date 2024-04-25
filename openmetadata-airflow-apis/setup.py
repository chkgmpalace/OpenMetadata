import glob
import os
import pathlib
from itertools import chain

from setuptools import find_packages, setup

PLUGIN_NAME = "openmetadata_managed_apis"
PLUGIN_ROOT_FILE = "plugin"
PLUGIN_ENTRY_POINT = "RestApiPlugin"
HOME = pathlib.Path(__file__).parent
README = (HOME / "README.md").read_text()
CONFIG = HOME / PLUGIN_NAME / "config.py"


def get_package_data():
    """
    Add templates and html files to the built
    package for easier deployments
    """
    extensions = ["html", "j2"]

    files = list(
        chain(
            *[
                glob.glob(f"{PLUGIN_NAME}/**/*.%s" % x, recursive=True)
                for x in extensions
            ]
        )
    )
    return [x.split(os.sep, 1)[1] for x in files]


def get_long_description():
    root = os.path.dirname(__file__)
    with open(os.path.join(root, "README.md")) as f:
        description = f.read()
    return description


base_requirements = {
    "pendulum~=2.1.2",
    "apache-airflow>=2.2.2",
    "Flask>=1.1.4",
    "Flask-Admin==1.6.0",
}

dev_requirements = {
    "black==22.3.0",
    "pytest",
    "pylint",
    "pytest-cov",
    "isort",
    "pycln",
}

setup(
    name=PLUGIN_NAME,
    packages=find_packages(include=[f"{PLUGIN_NAME}.*", PLUGIN_NAME]),
    include_package_data=True,
    package_data={PLUGIN_NAME: get_package_data()},
    version="1.3.3.2",
    url="https://open-metadata.org/",
    author="OpenMetadata Committers",
    license="Apache License 2.0",
    description="Airflow REST APIs to create and manage DAGS",
    long_description=get_long_description(),
    long_description_content_type="text/markdown",
    entry_points={
        "airflow.plugins": [
            f"{PLUGIN_NAME} = {PLUGIN_NAME}.{PLUGIN_ROOT_FILE}:{PLUGIN_ENTRY_POINT}"
        ]
    },
    python_requires=">=3.8",
    zip_safe=False,
    dependency_links=[],
    project_urls={
        "Documentation": "https://docs.open-metadata.org/",
        "Source": "https://github.com/open-metadata/OpenMetadata",
    },
    install_requires=list(base_requirements),
    extras_require={
        "base": list(base_requirements),
        "dev": list(dev_requirements),
    },
)
