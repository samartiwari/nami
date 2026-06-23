# Learnings

A search engine is made of just 3 simple parts

- Getting the data
- Strong the data
- Fetching the data

I am trying to make a wikipedia search engine

Can use *wikidumps* to download zip file of wikipedia data 
but its fixed meaning the data will be limited and if new 
pages are added i would not know

Better to use a **web crawler**:

### Web Crawler

Some type of bot which will be given a source url
eg: wikipedia/java.com
Crawler will:
- Fetch the html
- get all the information (title, description, links)
- then go to other links
- repeat

This maps out the whole web

#### Good internet citizen
Dont bombard the site with a lot of requests it might look 
like a ddos attack to them. Its best to:

- Crawl
- Sleep
- Repeat

#### Robot.txt
Some sites got a robot.txt file which is basically
for the robots like crawlers to instruct them
whats allowed and whats not.

#### Sitemap.xml
Some sites provide a sitemap file which provides
a list of all,most or some of the major pages on 
the website.
Crawling them one by one will efficiently map 
out the whole website.

### Processing
This is the part after crawling. The crawler fetches raw data. We
can either store it raw and use SELECT * WHERE LIKE "%JAVA%",
or process the data and store so its easier to fetch by the search 
engine.

#### Inverted Index
Every search engine use some form of inverted indexing to store 
pages.
Instead of storing title and description of page raw, it uses kind
of a map and store the keyword and in which page does it come in
eg:
- java = [1,3,5]
- spring = [1,4,8]
- flutter = [23,24,94]

so if it has 100 pages stored and someone searched spring, it won't
go through all the 100 pages just the map ['spring'].

Also, this process of building the inverted index is called 
**indexing**.

#### Storage
We store 2 things, one is inverted index and one is article
(title, snippet, url). The article is shown to the user on search.


We not storing full pages as we are not hosting them so no need to
store them fully.Just partial storage so we can show them as a result.
Also it will save a lot of space. We are just searching them for the
user. Once the user clicks on a link our work is over.

#### Text Processing
Before indexing we have to process the text so that we can get the
keywords.
We convert the whole text to lowercase and then we split it into words
We then remove the common words like "the", "a", "an", etc.
These are called stop words
We then remove the punctuation marks
We then convert the words to their root form
This is called stemming

#### Tokenization
Breaking down text into individual units
Eg: {"java", "spring", "is", "great"}

#### Stop words
Common words like "the", "a", "an", etc. These are removed.

#### Stemming
Converting words to their root form
Eg: Running, runner, ran -> run
--------------------------------------------

## Crawling

A web crawler is made up of a queue (which stores the sites
in the radar yet to be crawled) and a seen list (sites already
visited).

We start with a:

- queue = ['seed_url']
- seen = ['seed_url']

then a loop takes one url at a time from the queue and 
extract the data and put all the links in that data in the 
queue after verifying its not in the seen. (also update the
seen list)

It is like a script. Naturally you might python is better
suited.

### How to read robot.txt

- Find the block whose "User-agent" matches you.
- If you are not mentioned specifically you fall in User-agent*
- See the allowed and disallowed paths

Acceptable crawl rate for wiki is 1 req/sec

### How to make crawler

#### Jsoup - java soup
its a java library which has functions to pull out 
required things from html texts like .title or .body ect.
you can also fetch html with it. (a standard for crawling)

Its also a stanard to parse html. If a java developer needs to parse
html it will use jsoup. (It builds a kind of tree out of the html)
Tree as in html tag is parent and title and body are chilren like that...

#### When does a crawler start

- Start up (Command line runner)
- A get end point which trigger the run

The better options is an end point because using a CLR the app
is blocked for sometime during the startup (eg 17min)
during that time it wont accept any requests
Whereas with an endpoint the app is up instantly and the crawling
happen only on one thread and the server can accept requests on other 
threads.

Initially ill use the CLR as its an extra step to run and hit an end 
point right now while building the crawler.

#### Command line runner method

@Comonent for creating a bean. (managed by spring)

if you want something to run after startup like a bootstrap
you gotta have that in a class implementing 
commandlinerunner class.

there is a run method which needs to be overriden in 
the class implmenting clr which is called upon startup.

#### Start with crawler service

- Add the jsoup package in pom.xml
- Create the service
- Make service class public so i can call it from controller
- Call it from run method of class implementing clr

**Without initializing db app will crash if you have the 
dependency**

#### Now make bot better

- Have a name for it "namibot/0.1 (email)"
- This is the convention name/version (email)
- Now work on extraction of information not dumping full html
- Exrtation like taking the links and stuff

#### Extraction

- Checkout all the links
- Filter out the bad ones taking reference from robot.txt
- Currently ill dumb down the robot txt

Basically all the forbidden sites have ":" in them so ill use it
as a rule (this skip some real sites as well but very less)

Use unordered set as seen set for O(1) searching.
Then do the loop for the crawler along with 1s sleep

#### Exception handling
Since crawling is like a cron job not to give a response to someone.
A global exception handler is not required to handle exception.
Because no one is waiting for response.
Exceptions are handled differently here.

There are two types of exceptions:
- Checked - should be declared as throws when a method can throw it
- unchecked - throws declaration is not required

If you make a custom exception and extend Runtimeexception it 
becomes unchecked. No need to explicitly declare it in method
heading.

Also if a method throw any checked exception it needs to be 
declared in the header else it will show red line.
You cannot run app without actually declaring all the checked 
exceptions in method head, ide knows what it can throw.

After mentioning the exceptions a method can throw, write the
logic and if any exception happen (checked or unchecked) it will
bubble up.

Now the bubbling up exception needs to be caught somewhere 
in the app in any level else it will crash the app.

In the crawler we are catching the exception in the loop only.
we try the logic and if caught skip the link entirely.







