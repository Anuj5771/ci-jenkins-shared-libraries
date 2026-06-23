FROM python:3-alpine

ARG REQUIREMENTS_FILE_PATH=requirements.txt

WORKDIR /home

CMD --mount=type=cache,target=/root/.cache/pip pip install -r ${REQUIREMENTS_FILE_PATH}