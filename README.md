# bob

[![Github Build](https://img.shields.io/github/workflow/status/joffrey-bion/bob/CI-CD?label=build&logo=github)](https://github.com/joffrey-bion/bob/actions?query=workflow%3A%22CI-CD%22)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/bob/blob/master/LICENSE)

A CLI tool to help with management and maintenance of OSS repositories.

It can help setup secrets for a Github repository by fetching API keys from well known providers.

## Usage

You can run the program using [Docker](https://www.docker.com/) with the following command:

```
docker run --rm -it hildan/bob set-github-secrets [OPTIONS]
```

Please use `-h` for more information.

You can pass your GitHub login and API token on the command line as options, or you will be prompted to enter them.

## License

Code released under [the MIT license](https://github.com/joffrey-bion/bob/blob/master/LICENSE)
