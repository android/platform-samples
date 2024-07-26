# SaferIntents samples

This sample shows that it is important to not have a NULL intent.
It contains 2 buttons:

- One button sends an NULL intent
- Another button sends a regular intent

It is important to note that for the NULL intent an exception handling has taken place.
If an app sends a NULL intent it will crash with an `ActivityNotFound` exception