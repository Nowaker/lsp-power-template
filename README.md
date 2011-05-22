# Original LSP

[LSP][lsp] is an advanced web template language based on XML technology.
LSP provides powerful and easy to use presentation logic, but keeps
business logic and technical details out of templates. LSP is compiled
into Java bytecode for efficient execution.

# LSP Power Template

LSP Power Template is a fork of [LSP][lsp].
It aims to implement such features:

- hot-compile pages so that applications does need redeploy to see the changes
- introduce many "enclosing pages" - this can be understood as multiple template inheritance.
- be a substitute for kick-ass [Open Power Template][opt] (PHP) engine in Java world

# Why?

Java lacks a *good* template engine. This is what good template engine consists of:

## Static typing

In terms of templates, static typing can be considered as being XML based.
Well-formed XML templates guarantee valid XHTML/HTML5 output.

## XSS protection

Developer should never think of escaping every variable that comes to template.
People always forget about it. Each variable should be automatically escaped depending on context.
Compare to Apache Velocity.

    <p>
        $!generalUtil.escapeXml($!someVar) <!-- you surely forget about it one time -->
    </p>
    <p>
        <lsp:value-of select="$someVar"/>
    </p>

    <div class="$!generalUtil.escapeXmlAttribute($!someVar)" />
    <div>
        <lsp:attribute name="class" value="$someVar" />
    </div>

Developer should explicitly say if the content shouldn't be escaped.

    <lsp:value-if select="$someVar" disable-output-escaping="true" />

## Readability and pleasure

Templates should be well readable. Also, this should be fun to write them.
Do you like Apache Velocity mess?

    <div #if($first) class="#!firstClass" #end
         #if($last)  class="#!lastClass"  #end
    >
        ...
    </div>

    <div>
        <lsp:attribute name="class" value="$firstClass" lsp:if="$first" />
        <lsp:attribute name="class" value="$lastClass"  lsp:if="$last" />
        ...
    </div>


## Inheritance

layout.lsp:

    <html>
        <head>
            <title>
                <lsp:include part="title">Default title if title hasn't been overridden by child template</lsp:include>
            </title>
            <lsp:include part="head" /> <!-- if child template needs to add something here -->
        </head>
        <body>
            <div id="content">
                <lsp:include part="content" />
            </div>
            <div id="banner">
                <lsp:include part="banner" />
            </div>
        </body>
    </html>

articles.lsp:

    <lsp:root extend="layout.lsp">
        <lsp:part name="title">Articles</lsp:part>
        <lsp:part name="head">
            <link rel="stylesheet" href="/static/articles.css" />
        </lsp:part>

        <lsp:part name="content">
            All articles:

            <ul>
                <lsp:for-each select="$articles" var="article">
                    <li>
                        <a href="{$article.url}">
                            <lsp:value-of select="$article.title">
                        </a>
                    </li>
                </lsp:for-each>
            </ul>
        </lsp:part>

        <lsp:part name="banner">
            Some banner.
        </lsp:part>
    </lsp:root>

Spring Web MVC example:

    @RequestMapping("/articles")
    public ModelAndView show() {
        final List<Article> articles = articleDao.findAll();
        final ModelAndView mav = new ModelAndView("articles.lsp");
        mav.addObject("articles", articles);
        // ...
        return mav;
    }

# License

LSP is relesed under the modified BSD license, see LICENSE.



[opt]: http://www.invenzzia.org/en/projects/open-power-libraries/open-power-template
[lsp]: https://github.com/mikaelstaldal/LSP
