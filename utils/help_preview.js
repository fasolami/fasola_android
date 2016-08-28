$(function() {
    var USE_MARKDOWN = true;

    // Get and fix a document
    function getDocument(id) {
        return getDocumentText(id).then(function(text) {
            var $doc = $(text);
            // Fix images
            $doc.find('img').each(function() {
                var $img = $(this);
                var src = $img.attr('src');
                if (USE_MARKDOWN) src = decodeURI(src);
                src = src.split('|')[0];
                $img.attr('src', "../app/src/main/res/drawable-mdpi/" + src + ".png");
            });
            // Fix links
            $doc.find('a').click(function(e) {
                var href = $(this).attr("href");
                if (href.match(/^http/)) {
                    $(this).attr('target', '_blank');
                } else {
                    e.preventDefault();
                    history.pushState({id:href}, '', '?page=' + href);
                    navigateTo(href);
                    return false;
                }
            });
            return $doc;
        });
    }

    // Get document text

    if (USE_MARKDOWN) {
        // Markdown
        var md = markdownit();
        function getDocumentText(id) {
            return Promise.resolve($.get("md/" + id + ".md")).catch(function() {
                return Promise.resolve($.get("md/help_" + id + ".md"));
            }).then(function(doc) {
                console.log(doc);
                return md.render(doc);
            });
        }
    } else {
        // XML from android resources
        var xmldoc = Promise.resolve($.get("../app/src/main/res/values/strings.xml"));
        function getDocumentText(id) {
            return xmldoc.then(function(doc) {
                var text = $(doc).find('[name=' + id + ']').text();
                if (!text) {
                    text = $(doc).find('[name=help_' + id + ']').text();
                    if (!text)
                        return Promise.reject();
                }
                return text;
            });
        }
    }

    function navigateTo(state) {
        var id = state && typeof state === 'object' ? state.id : state;
        if (!id)
            id = 'home';
        console.log('navigate', id);
        getDocument(id).then(function(doc) {
            $('#page').html(doc);
        });
    }

    navigateTo(history.state);

    window.onpopstate = function(event) {
        navigateTo(event.state);
    }

});