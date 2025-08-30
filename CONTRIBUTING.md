# IoT DC3 Contributor’s Guide

Are you looking for ways to start contributing to IoT DC3? This guide will help you understand how to contribute to the project, giving you an understanding of the types of
contributions you can make, the standards for each type of contribution, the overall organization of the IoT DC3 project, and the contribution process.

## Types of Contributions

While one of the most common ways to contribute to open source software is through code, there are many other ways to participate in the community development and maintenance of
IoT DC3. In addition to code pull requests, you can contribute through bug reports, documentation fixes, feature requests, labeling templates, storage backends, and machine
learning examples. You can also participate in the IoT DC3 community Slack by engaging with the rest of the community and answering questions. No contribution is too small!

### Docs Update

One of the easiest ways to contribute to IoT DC3 is through documentation updates. Documentation is one of the first ways that new users will engage with IoT DC3, and should help
to guide users throughout their journey with IoT DC3. Helping to craft clear and correct documentation can have a lasting impact on the experience of the entire user community.

In addition to the change itself, docs updates should include a description of the documentation problem in the pull request, and how the pull request addresses the issue.

Use the Docs Update template for your pull request, and prefix your pull request title with `docs:`.

### Bug Report

Bug reports help identify issues the development team may have missed in testing, or edge cases that diminish the user experience. A good bug report not only alerts the development
team to an issue, but also provides the conditions to reproduce, verify, and fix the bug.

When filling out a bug report, please include as much of the following information as possible. If the development team can't reproduce your bug, they can’t take the necessary
steps to fix it.

A bug report can enter several different states, including:

- **verified**: The bug report has been verified and is in the development pipeline to be fixed
- **not a bug**: The report does not describe a bug, which might be the result of expected behavior, or misconfiguration of the platform
- **needs information**: The development team couldn’t verify the bug, and needs additional information before action can be taken
- **fixed**: The bug report describes a bug that has been fixed in the latest version of IoT DC3

When a bug report enters the “fixed” or “not a bug” states, the issue will be closed.

Use the Bug Report template for your issue.

### Bug Fix

Bug fixes build upon bug reports, and provide code that addresses the issue. Before submitting a bug fix, please submit a bug report to provide the necessary context for the
development team. Bug fixes should follow the coding standards for IoT DC3 and include tests. Unit tests are necessary to demonstrate the bug has been fixed, and to also provide a
safeguard against future regressions. In addition to unit tests, you should provide acceptance criteria that the QA team can use to verify the application's behavior. Bug fixes
must reference the original bug report.

Use the Bug Fix template for your pull request, and prefix your pull request title with `fix:`.
