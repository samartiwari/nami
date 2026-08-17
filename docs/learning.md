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

The Command line runner actually runs after app starts
it was a mistake earlier so i can crawl and search at the same
time.

    Version 2 complete

## Version 3

Now we focus on improve the result quality more by:

- Stemming
- Fuzzy Searching

First we will stem using lucene snowball library
basically a simple method which stems words.

Now for fuzzy searching we use postgres trigrams extention.
What it do is break the word into 3 letter words and make an 
array and compare.

eg:
if we search "sprng",
it will make [s,sp,spr,prn,rng]
and "spring" will have,
[s,sp,spr,pri,rin,ing]
postgres compare them and return if similar.

and this only runs if bm25 return 0 results.
(later can apply trigrams along with bm25 and find the best of 
both)

If result = 0, do the trigrams fetching of the nearest word,
if multiple words are wrong correct each word,
also if multiple words are close how do we deal with it?

Currently we will have the most similarity word wins,
later we will have the doc frequency (the most recurring word)
wins if the similartity is close of 2 or more words.

#### Full Architecture of Fuzzy search

Only work when searching return 0 result.
With a "Did you mean xyz?" banner

If 0 result is returned in a multi word search, meaning
none of the words found a place in the result meaning 
every word is incorrect.

So first we replace each word with the most similar word
using trigrams, and we keep a threshold value, so like
if none word matches in close proximity we will just use
the original word.
(so a word is not replaced with something very different)

Now if the corrected query == original query return 0 result

and if not then do the BM25 search on the new query and give the
result.

Now it has a limitation:
If search has many words and 1 word is correct and it gets us a result
and other doesnt, we essentially only searching for 1 word
We have to tackle that sooner or later.

#### Gin index
Currently the trigram will trigram my search words and brute 
force search the whole db of words of inverted index to find the 
closest trigram.
To speed that up we use gin index:
GIN = generalized inverted index
it maps the trigrams to words containing it
like for messi:
mes = [messi,mess,enzymes]
ess = [messi,mess,stress]

so first it seperate the search words into trigrams and then for each trigram 
find the words which contain it union them to get the set of actual candidated.
Then find the actual similarity percentage and return the max.

Honestly this model is not good, its returning similar but uncommon words
when challenged.
Like i searched "mussi" and its giving "musso" because it found one word 
To tackle that , for each word we will calculate the document frequency,
like in how many docs does it appear.
And have a threshold number to tackle the rare word case.

    Version 3 Complete

## Version 4

Now we add:
- pagination support
- caching
- search auto-completion

And this will conclude the features list for now, in later versions 
we will try to improve the features if we can.

### Pagination

We calculate the full result each time and slide it based on what page is asked.
This recalculation is avoided when caching.

### Caching 

For caching ill use redis.

**If i search a row in any table by its primary key, it will always be 
log(n) cause pk search is based on B-tree.**

#### Big change
Now instead of having an in memory seen map which cause scalibility issues,
we can blindly add all the urls in the DB. We will make the url field 
unique so same urls will not be added in the DB. Very smart.

## Wiki dumps

Now i think i need to test how fast if my search engine with big data,
like 200k,million etc number of articles.
So since i can only crawl at 11 days /million article speed i need to 
use already existing dataset to populate my db.

So we will be using a python script to feed the springboot the pages,
currently 1 at a time (later 100 or 1000 at a time) and springboot 
indexes them quickly without waiting 1 second because we are not making
a request to the actual server so we can be as fast as we can.

We will start with

### Ingest service

Basically copying all the previous service logic without the time delay.

We will use the existing repo to store pages,
and create just a new end point that the python script will hit.

Disable auto crawler for this test.
As it will pollute the db with its own crawling what we dont want.

#### Python script time

We install python venv and create a .venv folder in the root of the project.

venv = virtual environment
different project might need differnt python dependencies.
using venv solve that as all the python dependencies for that project
goes into that .venv folder and doesnt affect outside.

Now we install datasets and requests package for our venv.

Now actually write the loader.py which does not sit in venv.
It sits outside

Then run the app and then hit the python script.
It stored 100 articles under 30 sec.
Currently its slow because of
- We are pulling articles from hugging face one at
a time
- one http post per article to the springboot

We can download the articles and stream it locally,
it will be way faster.

We download one parquet file that is like having 
around 155k articles.
English wiki is 41 parquet files long.
Containing around 6.4M articles.
So we include downloading the file as well in the
python script.

Now we see the scalability issue:

Rare words like messi, photosyntesis are well under
50ms even with 50k articles in db, like time is not
growing.
But with common words like history, every thing has
kind of a history section.
So time grows, for 100k articles it takes 1 second
and its agonising when we reach 1m it will go to 
10 sec+.

Now we are shifting the bm25 ranking to postgres.
So postgres will find all the articles, score them , rank
them and give the result back to java .

- Java wont have to score 300k pages( for example) in a loop
- 300k pages transfer will not happen, only like 10 pages 
which is the result
- java doesnt deserialize 300k rows to java objects
only that 10 results.

So it cut a lot of cost.

Shifting to postgres helped a little
eariler at 50k article time to search history was over 500ms
now its a little over 200ms.
At 100k articles its like 350ms.

Now a little more optimization:

In the b tree of inverted index:

word    article_id     count

refer       3           42
messi       2           21
refer       53          12

so the b-tree was only indexed for word,
so when it was to find "refer", it would search the node 
then go back the the table to find the count.
Now we actually store the count and stuff in the node of 
b-tree so it doesnt have to go back to table again.

And for fuzzy search optimization, we add a new table which has
all the words and their df(document freq) precalculated so
its not calculated at runtime causing the time delay.
This table is not calculated for every article crawled.
Cause every article has around 900 unique words it will
be very time consuming so we have an endpoint which trigger 
the calculation.
We can manually hit the end point and it will also hit after
the crawl is complete once.
And also after every hour so i can use the feature during the
2-3 days of crawling as well.
This bring down the "messo" search from around 5-8s down to 
70ms. Thats huge.

Now optimizing about multi word,
if i stack multiple common words like
refer link extern includ time, time goes off the charts.
But no one search for something like that so i am leaving
this problem like that.
Maybe a later solve.
Solving it by adding those common words in stopwords list,
cause honestly they dont provide any information regarding 
the search them.

Now we will host the project,
a simple html,css, js page on cloudflare and apply
cors so only my website can use my backend.
Other people can still hit my end point with curl 
and postman but no other website can use my endpoint.
CORS is applied by the browser not by the backend.
Basically its the browser like chrome,bings decision to
not show a result when CORS is applied in backend, 
if you make a browser which doesnt respect this boundry 
then pages on your browser can hit any backend with cors.

CORS is triggered in cross origin

- scheme (http, https)
- host (site name like naukri.com, swiggy.com)
- port (8080, 5432)

if any of them is different we will hit cors 
by default cors block cross origin request it has to be
explicitly enabled.

CORS is configured in backend.

#### To upload it in oracle we need

- VCN
- Internet gateway inside it
- Route rule
- Subnet
- ingress rules (for port 80 , 443)

Now the more generous VM is very crowded and difficult to 
get my hands on.
So i am thinking of using 2 VM (1gb ram each)
1 for postgres+nginx and other for postgres.
Hopefully this will work.

#### Step 1

Create VM-1 for spring and nginx.
We get a private and a public key.
Public key goes to the VM instance like a lock and only
our private key can open it and help us to ssh into it.

Private key stays on my computer
Public key goes to the server.
The SSH refuses to use private key that have too open 
permissions basically if it can be read by everyone.
So we gotta chmod 600 so only us can r+W.

#### Step 2

Create the VM-2 for postgres
Reuse the ssh keys for this one.
Dont make this public facing, no one from outside can access.
Only my other vm.

So this will only have private ip no public ip.
So only my VM-1 can access it.
We use ssh agent forwarding to jumpt from VM1 to 2 without
copying my private key to VM1. (which can be a security issue)

But this makes it cut off from the internet as well.
So i am thinking of giving it public ip temporarily, 
to download required files then going private.
We add a ephermal ip address,
its an ip address attached to the instance, when the instance
dies it dies.(unlike reserved ip)

#### Step 3

Download postgres to vm2.
Create nami user
Create nami db
Enable trigram extention of postgres for my project.

By default postgres only listen on localhost.
So any fetching/storing can be done from vm2 only.
Basically **127.0.0.1**,
VM2 has **10.0.0.230** its private ip.
So we need to switch that up so postgres listen on the network.
listen_addresses = '*'.
After this postgres will listen to its localhost + private ip.
In oracle public ip is NAT'd meaning (Network address translation)
NAT means translating one ip to another when it pass through
a middle point.
And hence when we search for all the ips of a vm we only get 
2(localhost and private) not public as public ip of each vm sits
at the edge and forward the request to the private ip of the vm.
(managed by oracle not given to the vm)

For the whole VCN (virtual cloud network) its my own local network,
just like for my home my router and all the devices are a network,
i create a vcn in oracle and all the VMs inside it is part of the
network. 
This VCN will have a security list

- ingress rules => incoming traffic
any rule is just source(ip)+port
so like 0.0.0.0/0 means whole internet
port 22 means ssh
so a 0.0.0.0/0 port22 mean whole of the internet can ssh into 
our subnet
- egress rule => outgoing traffic
generally ports are not mentioned meaning they can reach to 
any port of that perticular source.

#### These specific rules make the firewall of the subnet,
#### These rules catch the request early in the outer parameter
#### of the network and never let them in if not allowed.   

Since postgres is listening to * its technically listening to 
the whole internet but due to the ingress rules of the subnet,
no internet request can pass the firewall. (as we dont have
0.0.0./0 5432 allowed)

Apart from that we have another firewall the second firewall,
**OS LEVEL FIREWALL**.
iptables => its a firewall build into linux kernel, every linux
machine has it, it filters packets at the OS level.
Deciding what traffic machine itself reject or accept.
So it filters traffic inside the VM.
When a VM is created only SSH(22) is allowed.
So the owner can ssh into the vm and change its settings and stuff.
**Only after opening both firewall can you reach 5432**
So i can stress my postgress but cannot connect to it yet.

pg_hba.conf file is there to tell postgres who is allowed
into the db, basically telling xyz ip can connect to
nami db and as which user.
Its very specific and acts as a last firewall for postgress.
So even if its completely naked(my db), others cant connect to
my db, and if they have the
user name and pass of the db as well they cant connect if they
dont have the exact ip mentioned in the conf file.

    host    nami    nami    10.0.0.0/24    md5

host-network connection
nami-db
nami-username
ip range allowed
md5- requires a password

## CIDR Notation (the `/` in IP addresses)

- **Core idea:** the `/N` turns a single IP into a **range** of IPs — it says how many bits are *fixed* (network part); the rest are free (host part).
- **Key rule:** bigger number after `/` = smaller/more specific range; smaller number = broader range.
- **IPv4 = 32 bits total** — the `/N` = how many of those bits are locked.

### Common values
- `/32` → **1 exact IP** (e.g. `10.0.0.202/32` = only that one address)
- `/24` → **256 IPs** — last number free (`10.0.0.X` = `10.0.0.0`–`10.0.0.255`) = a subnet
- `/16` → **65,536 IPs** — last two numbers free (`10.0.X.X`)
- `/0` → **the entire internet** (`0.0.0.0/0` = every IP, "anyone anywhere")

### Examples from my setup
- `0.0.0.0/0` → whole internet → used for public rules (SSH, HTTP)
- `10.0.0.0/24` → my private subnet (256 IPs) → both VMs live here
- `10.0.0.202/32` → one exact IP → only VM 1

### Notes
- **Name:** CIDR (Classless Inter-Domain Routing), said "cider" — standard for writing IP ranges (firewall rules, subnets, routing).
- **Mental shortcut:** `/N` = how "zoomed in" you are — `/32` = one address, `/0` = the whole internet.


Then we add a rule, to allow vm1 to connect with postgres in vm2,
without that postgres in vm1 will listen on the network but
reject all the requests. (pg_hba.conf file)

Then we restart postgres so it take the new configs into
consideration.

Have to read more about Computer Network

This level 1 firewall which we established at first is between VMs in the same
subnet as well, so VM1 cannot hit 5432 of another VM in the same subnet unless
the firewall 1 explicitly allows it.

So we add 
10.0.0.0/24 allowed to 5432 making all the ips in the subnet access the 5432 port
of each other.

iptables rules(OS level rules) are not persisted in reboot so we gotta write that in
netfilter-persistent save.

#### Step 4

Now we download java on VM1
SO we build the jar file locally
copy the jar file to vm1

You can override application.properties value from outside,
basically like environment variables.
So even if a perticular value is true in jar file we can make
it false from env.
We are capping the heap size to 512mb for springboot.
So it doesnt take a lot of ram we only have 1gb .
But we setting up env is not permanent and tied to the ssh connection.
To make env permanent we write it in systemd service file.

- systemctl daemon-reload → tells systemd "re-scan your service files" (so it notices the new nami.service)
- systemctl enable nami → "start this on every boot" (the auto-start-on-reboot part)
- systemctl start nami → start it right now

So even if vm restart nami will run automatically.

#### Step 5
Nginx reverse proxy to open the backend to the world.
So we install the nginx and start it 
Nginx auto start on port 80 (blocked by firewall 1 and 2)
We have the rules so firewall pass 80 and 443.
Then we add the rule in iptables on vm1 to allow those ports to the internet.

Then nginx becomes reachable from the internet.
Then configure nginx to direction requrests to port 8080.
And our backend become live <3!!!!.

Then we persist iptables.

Now we need to convert our backend from http to https.
Since our frontend is https it cannot call an http backend.
(its called mixed content and browser blocks it for security)

For making my backend https i need an ssl certificate only
given to names not bare ip so i need a domain name.

So i am using duckdns to get a free domain.
I named it "nami-domain.duckdns.org",
then point the domain name to my server ip

We install certbot and its nginx plugin
which provide 90 ssl certificate which auto renews.
As well upgrade any http to https.
For auto renewal of ssl, lets encrypt hit port 80 so 
i need to keep that open, and if someone hit my backend
at port 80 they will get unencrypted response, so 
we upgrade http to https.

Now how does http and https upgrade and mapping work to
my backend??

Now we update the backend url in the frontend and repush.
All good after that.
































