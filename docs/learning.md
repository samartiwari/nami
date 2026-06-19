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




