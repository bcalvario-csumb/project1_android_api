# Project 01 Post Mortem - Team 2 - project1_android_api
---
## Context 
---
We set to build an app that using the Any Crap API that make a card trading game. We wanted users to have a way to trade open cards and trade cards.

## By the numbers
---
Pull Request opened: 23 <br>
Pull Request mergered:  22 <br>
Issues Opened: 8 <br>
Issues Created: 26 <br>
Issues Closed: 18 <br> 

## What went well?
---
1. (Brandon) I think that our code structure and communication worked very well during the project, we all communicated in a timely manner and voiced our ideas and opinions. With the code structure, i don't think we had (or had very few) errors that would make us go on a scavenger hunt.

## What went wrong?
---
1. (Brandon) During the first week of the project, we encountered some problems with Gradle where for some of us it would build, but for others not. We managed to trace the issue to the fact that Android Studio doesn't add (and shouldn't add) specific files to the Git repository, so while one repository was updated with a specific version, that same repo would fail for another team member's device. Their local file displayed a different version for the depenency. Luckily, we were able to fix this by removing some files off the repository, so Gradle would be able to update itself on the local machine. The other contributing factor to this fix was when we were all able to update our branches to the bare bones structure of the project with what eacn member was implementing (such as the logins, room databse, and other features all combined so then all members have the same dependencies and versions.)


## Advice to our next teams?
---
