# Contributing

Thanks for considering a contribution. This is a small side project with one maintainer, so the process is kept simple.

## Workflow

1. Fork the repo (or branch directly if you have write access).
2. Branch off `main`: `git checkout -b fix/short-description`.
3. Make your change. Keep PRs focused — one backend or one bug per PR is easier to review than a grab-bag.
4. Open a pull request against `main`. The PR template will ask what you changed, which backend it touches, and how you tested it.
5. CI runs a debug build on every PR — it has to pass.
6. A maintainer review is required before merge (see [CODEOWNERS](.github/CODEOWNERS)). Direct pushes to `main` are reserved for the maintainer.
7. Once merged, a new [Release](https://github.com/duvanjamid/tv-remote/releases) is cut automatically with a downloadable APK/AAB — you don't need to do anything for that part.

## Code style

Nothing enforced by a linter yet — just match the style already in the file you're editing (Kotlin conventions, Compose idioms already used elsewhere in `ui/`).

## Testing

There's no emulator-friendly test suite for the TV-facing backends (SSAP/Android TV Remote v2/ADB all need real hardware to mean anything), so "how was this tested" in the PR template is doing real work — say what device/TV you tried it against, or flag clearly if you couldn't test it end-to-end.

## Reporting bugs / requesting features

Use the issue templates — they ask for the one or two things (which backend, which device) that make a report actually actionable.
