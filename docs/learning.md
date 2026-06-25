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

#### Snippet
This is the text you see as small description of the site.
Next try to extract that so later can be saved in db.
Its like 160-200 chars long.

For snippet, we can use first paragraph tag <p> we can find
but some wiki pages have notices as first paragraph tag
which reads "This is an accepted version of this page" or something
like this.

So its best to switch the selector from first paragraph to 
finding paragraph in the actual body of the wiki page.
Hence using div named "mw-parser-output" cause its specific for 
wikipedia and only contain body.

### Storage
Now will use postgres to store the crawled stuff.
Setup postgres docker container.

First we will set the application properties.

- Give a url to connect to
- A username and password as you always connect to the database as someone.
- Use the postgres driver (mentioned in the dependency)

Now i have to create a docker file so i can run an instance of 
postgres.
Setup properly the docker-compose.yml file.

**SHM SIZE** is a shared memory. By default its 64mb but can be set
to 128mb as i have done it.
This sets the size of a special type of memory a specific, special
region used for inter-process communication.
Like postgres have multiple processes like:

- postgres
- postgres: checkpointer
- postgres: background writer
- postgres: walwriter
- postgres: autovacuum launcher

each process work on their own private memory. but if any one 2 or 
more processes need to pass data to each other that is done
in shared memory.

**Adminer** is used to see inside the postgres database.
To check if things have saved or not.

#### Environment variable

Create a .env file which stores all the sensitive data like:

- DB_HOST=localhost
- DB_PORT=5432
- DB_NAME=nami
- DB_USER=nami
- DB_PASSWORD=nami

And docker files and application properties will read from it.
dockker compose file reads directly from any .env file present 
in the same folder whereas for the spring app i gotta load the 
.env in the terminal so application properties can use it.

We never commit .env file as it has secret passwords and keys.
We commit .env.example (basically a dummy) which has all the 
variables so people seeing the repo on git knows what they need
atleast to run the app.

#### Managing RAM

The seen set and queue is in RAM and will delete when app closes.
So we have to persist the seen as well as queue.

Upto 1 million pages it prolly fine to keep in RAM as it takes 
about 150MB-200MB.
More than that which is like google level it will not fit in
RAM.

They (google) use something called **bloom filter** basically
like it hashes the url and does something with it in binary form.
Bloom filter can give false positives but never false negative.
Like if bloom filter say i have never seen this url its 100% 
correct.If it says i have seen this url it CAN be wrong.

For this project lets cap the total pages around 1 million pages.
So bloom filters is not required.

#### Seen set
Since upon crawling we save each page to article table, upon restart
we will fetch from the article table and load the seen set
in the bootstrap logic. (or any other table where all the articles
exists)

#### Queue
Need a seperate table storing the queue and lets only have a 
db table storing the queue instead of in memory so it exists
after crashes as well.

We can also have an in memory queue as well which is loaded from 
the table of queue in bootstap for better performance but the 
performance boost is not as great compared to the complexity it
will introduce in the project.

#### Proceeding
- Make an article entity to store the id,title,snippet
- Make a frontier entity (basically queue in db) to store url,seen/pending
- Add respective repos and custom function in them as required
- Using 2 tables instead of 1 for cleanliness keeping completely crawled urls seperate from yet to be crawled urls
- Complete the service logic (which will be called from bootstrap)
- Load the .env to terminal then run (run docker before that)

If a containers ports have not been mapped properly
or you need to change anything you cant just change the docker
file and compose up, you have to compose down first to break down 
the container first. (basically recreate)

**Caught a bug** : The crawler treating sections of a page as different 
pages so messi, messi#early_life, messi#barcelona etc belonging
to the same page are considered differnt pages.
To fix, strip everything after #.

#### Inverted Index
Now instead of just storing the snipped we are processing the 
page and storing in inverted index now.

Now how do we store it actually in DB?

We will have an index table
- Id (generated)
- Word (eg: "messi", "java")
- article (the article its in)

So it will be each word takes multiple rows like

Id    Word    Article
1     Messi      3
2     Java       2
3     Messi      5...

Now if we search SELECT * WHERE Word = Messi, it will search
the whole DB which is bad so we "create index of word".
Now whats that?

It create a seperate B-tree (can have many children), of the words
and it becomes searching with a binary search instead of a linear 
search.
Root is the middle word (lexographically),
lets assume we are searching for "neymar", root is "messi"
then we know we need to go right because n>m so searching become 
log(n). Each node in tree points back to the table essentially helping
us search faster.

Just the trade off is insertion become slower as it will insert in 
the tree as well.

    indexes = @Index(name = "idx_word", columnList = "word")

Annotate your entity with this and it will do the trick.

In repository we use @Query annotation along with the 
spring method name magic as we need a leaner return type.
Without query specifically telling the return type should be 
only article ids it will return the full SELECT * type .
Basically the full row which is a little overkill.

    @Query("SELECT i.articleId FROM InvertedIndex i WHERE i.word = :word")

We use i. because Java SQL require alias.
its basically same as 
    
    SELECT articleId FROM InvertedIndex WHERE word = :word

Now create the indexing service which does
- Loading of stop words from a file i found on kaggle containing over 1000 stop words
- Lowercase the page
- Tokenize it (basically break into words about puntuations and spaces)
- put it in set to make words unique in a page
- Also make sure the words are not part of stop words else else skip
- batch insert all the word+article_id pair to inverted index table

Now in crawling a page we first save the snippet into article 
table, get the auto generated id, then use that in saving
to inverted index table

Now create a search service which takes search word, tokenize
it as well and find articles related to each word and return an
intersection of article.
eg: if we search "world cup"
it will find 
- world - 3,5,6,9
- cup - 3,6,7,13

Then return 3 and 6 as it contain both world and cup.
Very simple right now.

    Version 1 complete

## Version 2

Currently result of search is whatever crawled first, but now 
we have to make search result relevant as well.

One of the methods is **TF-IDF** :

- TF(term frequency), more number of searched words make the result more relevant.
- Basically if we search "messi", a site containing more number of "messi" is more relevant
- IDF (inverse document frequency), it searched the whole DB, if a word is contained in more number of 
documents, it makes the word less rare hence less relevant.


    IDF = log(total articles / articles containing this word)

eg: 
- "messi" in 15 out of 2400 articles → log(2400/15) = 5.07 (high — rare word, valuable)
- "football" in 2000 out of 2400 → log(2400/2000) = 0.18 (low — common word, not useful for ranking)


    TF  = count of word in article / total words in article
    or
    TF  = count of word in article

eg: TF  = 3 mentions / 2000 total words = 0.0015

    TF.IDF = TF X IDF

Now, TF is calculated during crawling, as we will know article 37 
contain messi 50 times during crawl so it will be stored in inverted
table, whereas IDF is calculated in search time as we dont know how 
many articles actually contain messi till we search.

When a search term contain multiple words, it calculate tfidf of
each word individually and sum them and show the result.

Previously we had AND logic searching, if we search "messi football",
we only get results containing both messi and football.

Now we will also get result containing messi or football ,
naturally the results containing messi and football will have
higher score of tfidf so they will be ranked higher.

In result also return the relevance score so i can show
something like "relevant" or something else in ui.

In single word searches, only tf determine ranking as idf is 
same for a word for all articles only tf changes.
TFIDF is only beneficial for multiple words.

But TFIDF have some problems:
- Word stuffing, an article containing only messi 1000 times will
be shown at top even though it doesnt do any good
- A 50k word document naturally contain more messi than 2k word
document, there is no normalization.
- Its not tunable, fixed formulas.

### BM25

    score(word, article) = IDF(word) × (tf × (k1 + 1)) / (tf + k1 × (1 - b + b × (dl / avgdl)))

    IDF(word) = log(1 + (N - df + 0.5) / (df + 0.5))

the +1 in formula is to prevent negative values as common words are
punished and if total number of pages crawled is less like 50 its 
highly likely a word is in most of them giving them a negative idf.

N = total article in DB
df = number of article containing this word

                 tf × (k1 + 1)
    ─────────────────────────────────────
    tf + k1 × (1 - b + b × (dl / avgdl))

tf = raw count of the word in this article
k1 = saturation parameter (default 1.2)
b = length normalization param (default 0.75)
dl = total word in this article
avgdl = avg total words in all articles

k1 is how quickly the tf saturation

- higher k1 means more mention scores higher
- lower k1 means tf stops caring as frequency increase

b is how much to penalize long document

- b=0 is ignore length
- b=1 fully normalize by length












