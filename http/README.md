# HTTP files

In this directory there lies .http files that can be used to execute http requests against the backend.

These requests rely on environment setup that can be aquired by running `ndla env http-client` if you have access to the `ndla` cli.

## Using the .http files

The files _should_ be usable with any software that supports `.http` files, but they were written and tested with [kualala.nvim](https://github.com/mistweaverco/kulala.nvim).
Also tested some with success with [httpyac](https://httpyac.github.io/).

Requests that does not require authentication worked fine in intellij as well, but the pre-request script failed in intellij for some reason (Feel free to explore).
