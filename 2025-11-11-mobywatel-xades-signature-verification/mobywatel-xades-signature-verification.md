# mObywatel XAdES Signature Verification

The [mObywatel](https://www.gov.pl/web/mobywatel) platform is a Polish government service that provides access to a [trusted signature](https://www.gov.pl/web/gov/podpisz-dokument-elektronicznie-wykorzystaj-podpis-zaufany)
for people who have a Trusted Profile (Profil Zaufany) — a Polish electronic identity system available to individuals with a PESEL number (the national identification number).
After a document is signed, it can be sent to other people so that they can verify it, using the mObywatel app as well.
One of the available formats is [XAdES](https://en.wikipedia.org/wiki/XAdES) which is the subject to this post.

The motivation for this post was to see whether ChatGPT could handle the task of verifying XAdES signatures step by step — something the author has been using as a personal “test challenge for ChatGPT” ever since ChatGPT became popular.
With each new version, the author spent some time trying to get ChatGPT to complete the task correctly — and for a long while, it simply couldn’t. The thing involves serveral steps which are de facto standard, however it was too much for earlier ChatGPT versions.
It turned out to be a nice challenge, since such a detailed, step-by-step walkthrough was nowhere to be found on the Polish internet (at least as far as the author could tell — and the internet is vast).
Only now, with the paid version of ChatGPT-5, did the model finally manage to produce a working solution — though not instantly, but after a series of increasingly precise prompts.
The scripts presented here were therefore written with the help of ChatGPT (v5 paid version - 'thinking mode').

The additional value for this post is **educational** one.
Its value lies in presenting the **entire process step by step**, using only simple command‑line tools.
For commercial use cases—such as verifying many files at once—the reader will likely choose ready to use libraries (commercial).
Indeed, the mObywatel app operates only on single files, and API access is not public (having the status of a state institution certainly helps).

**Warning!** The author of this post is **not** a security expert and assumes **no responsibility** for any use of the information presented here.

Given the number of steps involved, this post takes a minimalist approach.
Particular attention should be paid to the sections that compute digests, as they are the most sensitive—and the most frustrating—to implement by yourself.

## Tools
The author used:
```zsh
% sw_vers | awk '{print $2}' | paste -sd' ' -
macOS 15.6.1 24G90

% xmllint --version 2>&1 | head -1
xmllint: using libxml version 20913

% xmlstarlet --version 2>&1 | head -1
1.6.1

% openssl version -a 2>/dev/null | head -1
OpenSSL 3.5.0 8 Apr 2025 (Library: OpenSSL 3.5.0 8 Apr 2025)

% curl --version 2>/dev/null | head -1
curl 8.7.1 (x86_64-apple-darwin24.0) libcurl/8.7.1 (SecureTransport) LibreSSL/3.3.6 zlib/1.2.12 nghttp2/1.64.0
```

## Preparing the signed file
Create a file named `test.txt` with any content (e.g., `test`). Sign it using mObywatel, selecting the XAdES format.
As a result, you will obtain `test.txt.xml`.

**Warning! Do not modify this file in any way, otherwise you will not be able to recompute the digests (hashes)!**  
**Warning! The file contains your sensitive data, including your PESEL!**

It is a good idea to make a copy of the signed document and open it in your favorite editor so that the XML is neatly formatted.
This will make the following steps easier to understand.

## Verifying the signature
A complete set of scripts for the individual steps is placed in the [scripts](./scripts) directory.  
Begin by running the [verify.sh](./scripts/verify.sh) script, which invokes the steps one by one.

Assuming we are already in the `./scripts` directory and that our file `test.txt.xml` is inside it:

```zsh
./verify.sh test.txt.xml
```

## Summary
The post’s educational value lies in demonstrating, step by step, the entire signature verification process.
Commercial tools are available—but this may serve as a first step toward automating a useful idea, especially in open source.
Among interesting applications could be building a **system for voting** or **collecting signatures**.
