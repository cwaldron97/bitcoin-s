## Ammonite scripts

This project contain [Ammonite](https://ammonite.io) scripts that demonstrate
functionality of `bitcoin-s`.

#### Running them with sbt:

```bash
$ sbt "doc/run path/to/script.sc" # this is very slow, not recommended
```

#### Running them with the [Bloop CLI](https://scalacenter.github.io/bloop/):

```bash
$ bloop run doc --args path/to/script.sc # much faster than through sbt
```
