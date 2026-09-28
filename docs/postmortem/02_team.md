# Project 1 Post Mortem - [Group 2 / project1_android_api](https://github.com/bcalvario-csumb/project1_android_api)

## Context
We set to build an app that using the Any Crap API that make a card trading game. We wanted users to have a way to trade open cards and trade cards. These cards would take the title from the Any Crap API and use that as the name of the card, the image would be the picture that the card displays, and the description of the "AnyCrap" object would be the description of the card. Our API route would give us a random object from the API, which we would count as a "pack-opening", and display the card for the user to view, and then add to a collection of cards they could then view on the home page of the app. These cards would have randomized values associated with them not provided by the api so app would have some "gamification".

## By the numbers
- [Pull Request opened](https://github.com/bcalvario-csumb/project1_android_api/pulls?q=is%3Apr): 23
- [Pull Request merged](https://github.com/bcalvario-csumb/project1_android_api/pulls?q=is%3Apr+state%3Aclosed):  22
- [Issues Opened](https://github.com/bcalvario-csumb/project1_android_api/issues?q=is%3Aissue) : 26
- [Issues Closed](https://github.com/bcalvario-csumb/project1_android_api/issues?q=is%3Aissue+state%3Aclosed) : 18
- Planned at Kickoff: 14 stories | Done: 13
    
## What went well?
---
1. I think that our code structure and communication worked very well during the project, we all communicated in a timely manner and voiced our ideas and opinions. With the code structure, I don't think we had (or had very few) errors that would make us go on a scavenger hunt. 
2. Our code mostly merged smoothly together, and we did not have issues with people using different variable names from classes that were previously created, deleting imports, or working on the same file that would cause headaches for PRs. We all had a similar vision for the final product of our project and executed good chunks of work each week that would lead to something we are proud of.
---

## What went wrong?
---
1. During the first week of the project, we encountered some problems with Gradle where for some of us it would build, but for others not. We managed to trace the issue to the fact that Android Studio doesn't add (and shouldn't add) specific files to the Git repository, so while one repository was updated with a specific version, that same repo would fail for another team member's device. Their local file displayed a different version for the dependency. Luckily, we were able to fix this by removing some files off the repository, so Gradle would be able to update itself on the local machine. The other contributing factor to this fix was when we were all able to update our branches to the bare bones structure of the project with what each member was implementing (such as the logins, room database, and other features all combined so then all members have the same dependencies and versions.)
2. We had a couple of last minute merges/pull requests (one missing too) that could have been prevented. This was either due to a lack in communication that could have been avoided if we had more consistent check-ins with others' progress, or there was another situation where we all had a lapse in judgement due to a silent agreement that the lack of canvas-assignment for a pull request meant that we could have extra time to do the work. What we should have done is hold ourselves accountable to the work that was initially laid out at the start of the project.
---

## Advice to our next teams
---
1. We will commit ourselves to communicate daily, as well as speak about the progress on the branches we're working on.
2. Hold ourselves accountable to the schedule that is laid out at the beginning of the project, even if it seems on the surface that tardiness may not receive any penalties.
3. Continue looking at others' work as context for how your work will merge into theirs so that merge conflicts stay to a minimum and extra/repeated work does not occur unnecessarily.
---
