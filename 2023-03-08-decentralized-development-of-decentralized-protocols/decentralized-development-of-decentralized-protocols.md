# Decentralized development of decentralized protocols

The basis of contemporary decentralized protocols is often a set of documents called *improvement proposals*.
Often there are multiple implementations of the protocol and thousands of servers/clients form a decentralized network.
What is unsolved is the decentralization of the development process itself.
To be precise - the decentralization of the creation of those improvement proposals.

This article aims to propose a solution which *might* serve as an alternative.

## Is centralization unavoidable?
Some centralization is unavoidable - the word protocol itself indicates an agreement exists.

The hard part is figuring out what features are obligatory and should form the core protocol.
In the case of existing projects - such absolute basis can be set by trimming out an existing repo with improvement proposals.
The development of the rest should happen in separate repositories without being controlled by the core protocol developers.
Having a decentralized development process should make the whole development resemble a natural evolution process.
The core protocol developers will not decide whether given improvement proposal is a good fit or not.
They can of course work on the extensions (and most likely will).

## Development decentralization
The whole improvement proposal structure should form a loosely coupled structure resembling a tree.
The trunk is the core repository, with the extensions repositories forming branches.
This is a natural formation taking into consideration interoperability.

**Note**: the branches here are not version control branches! What is more, the control version is not set to git.
The version control tool is up to the developers. Various teams might choose different solutions.
A tree would be formed naturally by forking repositories, but there is nothing like that imposed.
One can imagine a situation where a new group of developers not only forms a new branch (i.e. a new repo) by adding proposals but also removes some of them/creates a mix of branches.

**Note 2**: two branches which do not have a common ancestor can still be interoperable - it all depends on the improvements proposals they are formed of.
When one feels there is a need to create a new extension (i.e. write down a new improvement proposal),
one can create a new repo and copy some of the existing improvement proposals.
Which exactly? Well, the core is mandatory and the rest is optional. Whatever one finds suitable.

The following scenarios are possible here:  
1. A new improvement proposal for a server - one creates a new repo (updates existing) by putting the core set of improvement proposals plus other existing
   improvement proposals implemented by their server. Including the one(s) proposed by them.  
2. Same as above but for a client.  
3. Having a good idea for a new proposal but unwilling/unable to implement it - approach others.


## Core repo organization
The rules to organize the *core repository* would be:  
- Agree on a bare minimum for the set of improvement proposals.  
- The fact the improvement proposals form the core means they should be modified as rarely as possible.  
- One of the core proposals should be that servers/clients provide a list of implemented improvement proposals.  
- It should be clearly stated that the core repo is just the core and how to develop new proposals.  
- Numbers should be avoided to enumerate proposals, which can easily create conflicts. FIP-xyz-description should do fine for the protocol Foo Improvement Proposal.  
- There is no need to version proposals themselves. If there is a new version, a new proposal should be created.  
- The core repo should not point to any of the extension repos to avoid favoring them.
   The extension repos will be easy to find anyway, using search boxes like the ones offered by GitHub/Google etc.  
- A proper open-source license should be chosen. Putting a clause saying that when just copying improvement proposals would be worthwhile.  

## Summary
Hopefully, by adopting the development process similar to the one above, the development process will be done in a decentralized fashion.
Hopefully, new proposals will pop up and then, after being battle tested, will be adopted by others.
This process should promote ideas that turn out to be great and cause going into oblivion ideas that turn out to be bad.
Like natural selection. Rules are not strict - each community is encouraged to adapt them to their needs.
