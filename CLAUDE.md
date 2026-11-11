# Working rules for Claude Code

The full specification is in `Spec.md` (the spec calls it `SPEC.md`). These rules apply in every session.

## The owner

- The owner is experienced in release management and CI/CD, and is new to Spring Boot, JPA and Kafka.
- The purpose is learning. Explain each Spring, JPA or Kafka concept in two or three sentences the first time it appears, in chat, as you write the code.

## How to work

1. Work on one milestone at a time. Never start the next one unprompted.
2. Before writing code, state the plan for the milestone in ten lines or fewer.
3. Build in small steps and run the tests after each one.
4. Finish by running `./mvnw verify` and each acceptance check, and show the real output. Report a failing check as failing.
5. Write `docs/decisions/NN-<milestone>.md`: what was built, the three most important design decisions, the alternative rejected for each, and how a production team would likely differ.
6. Update the README so that its commands are always correct.

## Limits

- Add nothing that this specification does not ask for: no extra endpoints, libraries or abstractions.
- If the specification is ambiguous, or a library does not support the chosen versions, stop and ask.
- Never write a secret, token or account id into a file. Never weaken a test to make it pass.
- Do not run `git push`, `terraform apply` or `terraform destroy`. The owner runs those.
- Commit locally in small commits with messages in the form `feat: ...`, `test: ...`, `chore: ...`.

## Code style

- Constructor injection only; no field injection.
- Records for DTOs and events; no Lombok.
- Transactions on service methods, not on controllers or repositories.
- No comments that restate the code. Comment only the reason for a non-obvious choice.
