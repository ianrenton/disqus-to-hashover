# disqus-to-hashover
A quick and dirty Java app to convert a Disqus export file into HashOver's file structure.

Includes Disqus' own schema for the file format, which I've had to modify slightly to make it compatible with JAXB.

Released under the terms of the BSD 2-clause licence.

**Known issue:** Disqus exports links as standard HTML, i.e. `<a href...`. but when HashOver does its rendering, it expects to see URLs as plain text, and will render badly if it finds URLs inside an `a` tag. My comments only had a few links in so i juat fixed them manually, but if you have a lot, you may want to modify the script so that it spots the tags and converts them to a plain URL.
