# Assignment 6

_Kristian Moltke Reitzel, krei@itu.dk_

## Problem 1: Basis of Trust

### Part 1
`<B, ax, Amasoft Inc.>`
- Unreasonable because there is no proof that Amasoft Inc. is trustworthy.
### Part 2
`<B, ax, Amasoft Inc.>`, `<T, an, me>`
- Somewhat reasonable assuming I can comprehend ALL reasonable inputs (i.e. the program is fairly simple).
### Part 3
`<B, ax, Amasoft Inc.>`, `<P, an, V>`, `<V, ax, me>`
- Pretty unreasonable since I blindly trust the third-party analytical program. However, if I have analyzed V to be a trustworthy program (i.e. `<V, an, me>`) then it would be somewhat reasonable.
### Part 4
`<B, ax, Amasoft Inc.>`, `<P, an, V>`, `<V, an, me>`
- This is reasonable since I have analyzed the third-party program and then uses it to verify `P` (i.e. this is kinda transitive).
### Part 5
`<B, ax, Amasoft Inc.>`, `<B, sy, B'>`, `<B', ax, me>`
- This is unreasonable since I blindly trust the trustworthiness of the third-party program, `B'`
### Part 6
`<Amy, an, M>`, `<M, sy, compiler>`, `<compiler(writer), ax, me>`
- This is unreasonable because 1) Amy might be malicious and 2) the compiler(writer) might be malicious.
### Part 7
`<M, an, Bob>`, `<Review, ax, me>`
- This is unreasonable since I blindly trust Bob's code review. He might be malicious or his review might be incomplete.
### Part 8
`<T, an, Amy&Bob>`, `<T, an, me>`, `<B, an, T>`
- This is reasonable since I have checked `T` to be trustworthy and then I use `T` to analyze `B` (Again transitive)
### Part 9
`<M, an, Amy>`, `<M, sy, commons-crypto>`, `<M, an, me>`, `<commons-crypto, ax, me>`
- This is unreasonable since I blindly trust that `commons-crypto` is trustworthy - it could change `M` maliciously even though it's just a thin wrapper.
### Part 10
`<M, an, Amy>`, `<M, an, me>`, `<M, an, Paragon>`, `<Paragon, ax, me>`
- This is _plausible_ reasonable, since Paragon is a dedicated extension to Java to help implement IFC. However, Paragon might of course have unknown vulnerabilities that I'm not aware of.

