# Install

```
$ sbt pack
```

Add `target/pack/bin` to `PATH`

# Usage

**NOTE**: Only available for JSON-LD

## Single file mode

```
$ sem-diff -left /path/to/left.jsonld -right /path/to/right.jsonld
```

or 
```
$ sem-diff -l /path/to/left.jsonld -r /path/to/right.jsonld
```

## Bulk mode

```
$ sem-diff -left /path/to/left -right /path/to/right -paths-file /paths.txt
```
or 
```
$ sem-diff -l /path/to/left -r /path/to/right -pf /paths.txt
```