# bob

[![Github Build](https://img.shields.io/github/actions/workflow/status/joffrey-bion/bob/build.yml?label=build&logo=github)](https://github.com/joffrey-bion/bob/actions/workflows/build.yml)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/bob/blob/master/LICENSE)

A CLI tool to help with management and maintenance of OSS repositories.

For instance, it automates the setup of secrets for a set of Github repositories.

## Usage

You can run the program using [Docker](https://www.docker.com/) with the following command:

```
docker run --rm -it hildan/bob set-github-secrets [OPTIONS]
```

Please use `-h` for more information.

You can pass your GitHub login and API token on the command line as options, or you will be prompted to enter them.

## License

Code released under [the MIT license](https://github.com/joffrey-bion/bob/blob/master/LICENSE)
