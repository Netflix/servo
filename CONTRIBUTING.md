## Contributing to Servo

If you would like to contribute code, then you can do so through the following process:

* Fork the repository on GitHub and clone your fork.
* Configure the upstream remote, so that you can keep your fork synchronized.
```
git remote add upstream https://github.com/Netflix/servo.git
```
* Create a branch for your changes.
```
git branch my-awesome-bug-fix
git checkout my-awesome-bug-fix
```
* Check for and merge upstream changes.
```
git fetch upstream
git checkout master
git merge upstream/master
git checkout my-awesome-bug-fix
git rebase master
```
* Build your changes and run unit tests.  You will see many warnings and errors because Servo unit tests validate error conditions.  As long as the final result is BUILD SUCCESSFUL, then the tests have passed.
```
./gradlew build
...
BUILD SUCCESSFUL
```
* Push your changes and send a pull request.
```
git push origin
```
* Watch for the `cloudbees-pull-request-builder` to comment on the status of your pull request.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible.

## License

By contributing your code, you agree to license your contribution under the terms of the [APLv2](https://github.com/Netflix/servo/blob/master/LICENSE).

All files are released with the Apache 2.0 license.

If you are adding a new file it should have a header like this:

```
/**
* Copyright 2014 Netflix, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
```
