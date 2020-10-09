## Setup and deploying on Tomcat

* Download Tomcat (version I am using -  9.0.38):

``brew update``

``brew install tomcat``

* Development and compilation was done with Java 11, it could be necessary to install it, too.

``JRE_HOME: /Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/Contents/Home``

* Get the paths for you Tomcat installation:

``brew ls tomcat``

* Copy `redditsubmissions.war` into Tomcat installation folder `/webapps` :

``cp ./redditsubmissions.war /usr/local/Cellar/tomcat/9.0.38/libexec/webapps/redditsubmissions.war ``

* Run tomcat server startup script (``startup.bat`` for windows)

``sh /usr/local/Cellar/tomcat/9.0.38/libexec/bin/startup.sh``


## API endpoints and parameters

There are 3 endpoints:

``http://localhost:8080/subredditsubmissions/reddit/activity?interval={interval}`` - returns comment and submission count- `{"comments": 12 ,"submissions": 12 }`  - both values are of long type.

``http://localhost:8080/subredditsubmissions/reddit/subreddits?interval={interval}`` - returns top 100 most active subreddits and their activity - `[{"name":"AAA", "activity":12},{"name":"ABB", "activity":12}...{"name":"AAB", "activity":11}]` - `name` is `String` and `activity` is `long`

``http://localhost:8080/subredditsubmissions/reddit/users?interval={interval}`` - same return structure as for subreddits, but data returned is for top 100 most active users.

### Interval parameter
Parameter `interval` can be used to filter results for a set time that has passed.
If passed value cannot be identified, all the possible options will be returned.

Possible values are:
* minute
* five_minutes
* hour
* day
* all_time

Furthermore, if `interval` is not passed, results will be the same as for `all_time`

## Challanges, Problems, what could be improved

#### Implementation of filtering data by interval

During development I decided to put all received event data in one file, which I would use to parse values for `minute` - `day` intervals.

File with all events would become too big over time and make the API slower, therefore I decided to store return values for `all_time` interval in separate files as key, value pairs. As well, as deleting rows that are older than 24 hours for the main events files.

While this worked correctly during testing with IDE built-in running of Tomcat, I noticed some issues when deploying `.war` file directly in tomcat - the `all_time` results were much smaller compared to those filtered by intervals. I have a suspicion that it might be related to some file write access restrictions during Tomcat start up, but I have not investigated the issue yet.

#### Issues with stream.pushshift.io

During first 2-3 days after receiving the assignment I was not able to receive any other events than "keepalive". After some research, I found out that there have been couple of issues with the performance during last months. I create a Reddit thread in /r/pushshift , however, I was able to retrieve `rc` and `rs` events on 4th day.

#### Possible improvements

I used a file system persistance layer for saving only the information that is necessary for endpoints in CSV format. As this would save some storage and setup time, the solution is not very expandable. For example, it would be hard to add a new endpoint that would return content of comments, as that would require a lot of  changes in code. 

Although it did the job and fulfilled the requirements, I think a better solution would have been to use a database layer instead. That would make the code cleaner and solution more expandable.



