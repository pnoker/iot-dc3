---
title: License
---

# License

This page is for developers, legal reviewers, and decision-makers who need to understand what license IoT DC3 uses,
what rights you have, and what obligations it imposes.

> You are here: evaluating whether to adopt or redistribute. For key technical decisions, also
> read [Core Concepts](./concepts) and the [Contributing Guide](../community/contributing).

## License

IoT DC3 Community Edition is licensed under the **GNU Affero General Public License v3.0 or later**
(AGPL-3.0-or-later). The full legal text is at the repository root in `LICENSE-AGPL.txt` and `LICENSE.txt`.

AGPL v3 extends GPL v3 with one critical clause: **if you serve the software over a network (SaaS), your modified
source code must also be released to users**. This is directly relevant to IoT DC3's role as an industrial IoT platform.

| You can                                                  | You must                                                                                                          |
|----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| ✅ Commercial use                                         | ⚠️ Keep the copyright notice and license text intact                                                              |
| ✅ Modify the code                                        | ⚠️ Release modifications under AGPL v3 as well                                                                    |
| ✅ Distribute internally                                  | ⚠️ If you provide the software as a network service (including SaaS), you must make the complete source available |
| ✅ Offer paid services, operations, or custom development | ⚠️ Include a prominent notice that the code is AGPL v3 licensed, along with the license text                      |

::: warning Network interaction triggers copyleft
This is the key difference between AGPL and GPL: GPL triggers the source-disclosure obligation only when you
"distribute binaries"; AGPL triggers it when users interact with your modified version over a network. In other
words, even if you deploy IoT DC3 as an internal SaaS without distributing binaries, if you modified the code you
must make the source available to your users.
:::

## Copyright

```
Copyright 2016-present the IoT DC3 original author or authors.
```

The project is copyrighted by the IoT DC3 original author and all contributors. By submitting code, you agree to
license your contribution under AGPL v3 while retaining your individual copyright.

## Third-Party Dependencies

IoT DC3 depends on many open-source components (Spring Boot, RabbitMQ, PostgreSQL, Netty, gRPC, etc.), each carrying
its own license. Maven resolves them at build time, and each is governed by its respective license. For a complete
inventory of third-party dependency licenses, run:

```bash
mvn -s .mvn/settings.xml license:aggregate-add-third-party
```

## Why AGPL v3

Typical industrial IoT platform deployments — factory on-premise installation, device connectivity, data collection —
are naturally server-side deployments. We chose AGPL v3 to:

- **Prevent closed forks**: vendors can't take IoT DC3, modify it slightly, and ship it as a proprietary product
  without releasing source.
- **Protect user rights**: anyone using a derivative of IoT DC3 has the right to obtain the source code.
- **Encourage upstream contributions**: AGPL's copyleft scope gives companies a strong incentive to push changes
  upstream rather than maintaining private forks.

## Further Reading

- [Contributing Guide](../community/contributing) — how to submit code, with license compliance notes
- [COPYRIGHT file](https://github.com/pnoker/iot-dc3/blob/main/COPYRIGHT) — the original copyright notice at the
  repository root
- [AGPL v3 FAQ](https://www.gnu.org/licenses/agpl-3.0.html) — official GNU FAQ
- [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/) — the upstream Code of
  Conduct reference
