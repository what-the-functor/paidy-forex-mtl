# Paidy Forex Exercise - Tony Merlin

## Requirements
The project requrements are echoed here:

* The service returns an exchange rate when provided with 2 supported currencies 
* The rate should not be older than 5 minutes
* The service should support at least 10,000 successful requests per day with 1 API token

The upstream one-frame API supports a maximum of 1000 requests per day, per auth token.  The definition of *day* is assumed to be: a 24-hour period from first request for the given auth token.  
For more information, see [Forex.md](./Forex.md).

## Analysis

The service will request rates as a batch, and cache the set of rates on the server.  
This will limit requests to the upstream one-frame API, to help with: the rate max age, and the upstream limitation of 1000 requests per api token.

### Rate Max Age: 5 minutes
We can ensure that any rate served to the client is not older than 5 minutes; there is no apparent way to ensure that the client does not receive a rate older than 5 minutes.  This is due to network latency, and application performance.

My idea is to handle the max age with a 5 minute time-to-live (TTL) setting for rates cached on the server-side, and add `Cache-Control` headers in the response to the client. 

The 5 minute TTL ensures that no rate is older than approximately 5 minutes when it is retrieved, although it does not account for network latency, and server performance.  
For example, a request is received which results in a cache hit for a rate that is aged, 4 minutes and 599 milliseconds.  It is highly probable that, by the time the client receives the response, the rate is older than 5 minutes.  

The TTL could be offset, based on the request timeout, however the request time out is controlled by the client, not the server.  
Rather than offset the cache TTL, the response will include the `Cache-Control` header with the `max-age`, `no-cache`, and `private` directives.  
This will help to ensure that, if the client receives a stale rate, the client will not use it (provided that the client obeys standard HTTP Cache-Control headers).
Each response's `max-age` *M2*, will be set to the difference between the 5m max, *M* and the rate's age on the server, *A*.
`*M2* = *M* - *A*`  
There is an edge-case wherein the client could reveive a response that is already stale, if the network latency is greater than`max-age`.  

### 10,000 successful requests per day given upstream limit of 1000 requests per day
The one-frame service supports up to 1000 requests per day, though we can request multiple rate pairs per request.

```
GET /rates?pair={currency_pair_0}&pair={currency_pair_1}&...pair={currency_pair_n}
```

There is no limit to the length of a query string according to the HTTP/1.1 standard, [RFC 2616](https://www.rfc-editor.org/rfc/rfc2616#section-3.2.1).
We can simply batch requests to the one-frame service, and cache the results.

Assuming that there is no limit to the number of pairs that the one-frame service will serve in one request, we can request all pairs at once.  If all pairs are queried at once, every five minutes, for 24 hours, there will be 288 requests to the one-frame service.

The number of pairs that are requested, can be reduced by eliminating any pairs of the same currency.  The rate is always 1:1.  We can request 72 pairs at once.

Given that the `/rate` endpoint is only concerned with the price, we can further shorten the query string to 8 pairs (reasoning explained under [Shortening the query string](#shortening-the-query-string)).

#### Scenario 1

A client requests a new rate for the same pair, every 5 minutes.  

#### Scenario 2

A client requests all possible rates simultaneously, at 5 minute intervals.  

#### Shortening the query string

There are 9 (n) different currencies, which (naively) results in 81 possible pairs, n^<sup>2</sup>.
```
9^2 = 81
```

We can eleminate a currency paired with itself (for example JPY to JPY), as the rate is always 1:1.
For any given currency, there is exactly one case where it can be paired with itself, so out of 9 currencies there will be 9 self-pairs.  We can simply subtract 9 from our total of 81 pairs to arrive at 72.
```
(9^2) - 9 = 9 * 8 = 72
```

Although, the one-frame service returns the *price*, *bid*, and *ask* values for a rate pair, the `/rates` endpoint of our service returns only the *price*. 
Given this simplification:
- a rate can be [derived from its inverse rate](#derivation-of-inverse-rates), and
- rates can be [derived based on a standard rate](#derivation-from-standard-rates).

#### Derivation of inverse rates

We can eliminate pairs wherein the rate can be derived from its inverse rate.
For example, if we have the rate CHF to JPY, we can derive JPY to CHF as the inverse.
There will be exactly 9 cases wherein a rate can be derived.  Again, we can subtract 9 from the running total of 72, to arrive at 63.
```
(9^2) - (9 * 2) = 9 * 7 = 63
```

#### Derivation from standard rates

We can derive a FX rate between any two currencies from their rate to/from a standard currency. The FX standard currency is US dollars.  
For example, the rate AUD to CHF can be derived from the rates: AUD to USD, and CHF to USD.
Therefore, for any given currency, we only need to know the its rate to USD.

This results in 8 pair queries, and now the query string is reasonably short.

To ensure accuracy, rates should only be derived from rates of which are all the same age (or within a specific tolerance).  The age of the derived rate should be that of the oldest rate from which it was derived.
Rates to USD will be requested all at once, at intervals of less than 5 minutes; thus all cached, and derived rates will be the same age.

## Implementation

### Caching
Caching comes down to three choices: *Scalacache*, *Jujiu*, and *Mules*.  
*Scalacache* has been around for years, and has support for cats-effect, however I dislike that the examples in the documentation use the `Try` type (which should not be used for effects).  
*Jujiu* is arguably more purely functional than *Scalacache*, and I've used it before.  
I chose to use *Mules* due to the fact that there is good interop with the Typelevel ecosystem, and the contributors are familiar.

### Rate batching

There are 1440 minutes per day; with 288 consecutive periods of 5 minutes. This is far below the one-frame limit of 1000 requests per day.  
The batch interval is reduced, in order to provide a better customer experience, and maximise the usage of the one-frame service.  

## About my development environment

I use [Metals](https://scalameta.org/metals/), [direnv](https://direnv.net), and [Nix](https://nixos.org).

### What is the `.envrc` file?

It's a configuration file for `direnv`; think of it like virtual environments for Python, but for anything (and better).
You can ignore this file, or check out the [Nix flake](https://github.com/what-the-functor/nix-scala-minimal) if you're curious.

## About this repoository

This repo is split from the `paidy/interview` mono-repo.

### *Why* did you do it?

Metals likes a `build.sbt` file at the root of the project.
Thus, my options were...
1. Add a `build.sbt` at the project root, which references `forex-mtl` as a submodule.
2. Split the `forex-mtl` directory into a new repository, making it the new project root.
3. Copy the `forex-mtl` folder without git history, and create a new repository from it.
4. Use a different IDE.

*Option 1* adds additional complexity to the build, though in some-cases could be prefereable; this is my runner-up choice.  
*Option 2* keeps the git history, which is important.  
*Option 3* is quick, (and dirty) the git history is lost.  
*Option 4* is a last resort.  

I went with *Option 2*, which as you will see, doesn't add many steps as compared with *Option 3*.

### *How* did you do it?

#### Clone the parent repo

A normal clone is sufficient.

```shell
git clone git@github.com:paidy/interview.git paidy-forex-mtl
```

#### Move Forex.md
I moved the `Forex.md` file to the `forex-mtl` subdirectory, so that it's present in the new repo.

```shell
git mv {,forex-mtl/}Forex.md
```

Side note: I like to rename (move) files on distinct commits which don't include any changes to the contents of the files.
This makes it easy to tell simple renames apart, from commits which change content.

#### Split the subdirectory

This creates a new branch `forex-mtl/master`, which contains the commits on `master`, including only the `forex-mtl` *subtree*.  
In other words: only the files starting from `forex-mtl`, and their changes on the `master` branch.

```shell
pushd !$
```

```shell
git subtree split -P forex-mtl -b forex-mtl/master
```

#### Create the new repo

```shell
mkdir ../paidy-forex-mtl
```

```shell
pushd !$
```

```shell
git init
```

```shell
git pull ../paidy-interview forex-mtl/master
```

At this point the new repo is ready to use.
