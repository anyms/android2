!(function() {


    $ = {};



    /* Element Finder */
    $.element = {};

    $.element.findTree = function(el) {
        let currentEl = el;
        const tree = [];
        let nodes = Array.prototype.slice.call( currentEl.parentElement.children );
        let nth = nodes.indexOf(currentEl) + 1;
        tree.push({
            tagName: currentEl.tagName.toLowerCase(),
            nth: nth,
            attrs: $.element.attrToArray( currentEl.attributes )
        });

        while (true) {
            currentEl = currentEl.parentElement;
            nodes = Array.prototype.slice.call( currentEl.parentElement.children );
            nth = nodes.indexOf(currentEl) + 1;
            const parentTagName = currentEl.tagName.toLowerCase();
            tree.push({
                tagName: parentTagName,
                nth: nth,
                attrs: $.element.attrToArray( currentEl.attributes )
            });
            if (parentTagName == "body") {
                break;
            }
        }
        tree.reverse();

        return tree;
    };

    $.element.attrToArray = function(attrs) {
        const arr = [];
        for (let i = 0; i < attrs.length; i++) {
            arr.push({
                name: attrs[i].name,
                value: attrs[i].value
            });
        }
        return arr;
    }

    $.element.concatTagNames = function(tree) {
        let s = "";
        for (let i = 0; i < tree.length; i++) {
            s += (tree[i].tagName + " ")
        }
        return s.trim();
    }

    $.element.concatAttrClassNames = function(attrs) {
        let s = "";
        for (let i = 0; i < attrs.length; i++) {
            if (attrs[i].name === "class") {
                s += (attrs[i].value + " ")
            }
        }
        return s.trim();
    }


    $.element.concatTagNamesWithNth = function(tree) {
        let s = "";
        for (let i = 0; i < tree.length; i ++) {
            s += (tree[i].tagName + " " + tree[i].nth + " ")
        }
        return s.trim()
    }


    $.element.findSimilar = function(tree, doc) {
        const founds = [];
        const otherEls = doc.querySelectorAll(tree[tree.length - 1].tagName);
        const tagString = $.element.concatTagNames(tree);

        for (let i = 0; i < otherEls.length; i++) {
            const otherTree = $.element.findTree(otherEls[i]);
            const otherTagString = $.element.concatTagNames(otherTree);
            // concatTagNamesWithNth(tree) !== concatTagNamesWithNth(otherTree) && 
            if (!$.element.isHidden(otherEls[i]) && tagString === otherTagString && $.element.concatAttrClassNames(tree[tree.length - 1].attrs) === $.element.concatAttrClassNames(otherTree[otherTree.length - 1].attrs)) {
                //console.log(tagString, otherTagString);
                // console.log(doc.querySelector(toSelector(otherTree)));
                founds.push(otherTree);
            }
        }

        return founds;
    }

    $.element.isHidden = function(el) {
        return (el.offsetParent === null);
    }

    $.element.treeToSelector = function(tree) {
        let s = "";

        for (let i = 0; i < tree.length; i++) {
            s += (tree[i].tagName + ":nth-child(" + tree[i].nth + ") ");
        }

        return s.trim();
    }



    /* Event Handler */

    $.event = {};
    $.mouseEvent = {};

    $.mouseEvent.trigger = function(el, e) {
        const event = new MouseEvent(e);
        el.dispatchEvent(event);
    }

    $.event.trigger = function(el, e) {
        const event = new Event(e);
        el.dispatchEvent(event);
    }


    

    function bind(doc, isIframe, iframeIndex) {
        doc.addEventListener("click", function(e) {
            if (!$.element.isHidden(e.target)) {
                const element = {};
                const treeList = $.element.findTree(e.target);
                const selector = $.element.treeToSelector(treeList);
                const similar = $.element.findSimilar(JSON.parse(JSON.stringify(treeList)), doc);
                element.tree = treeList;
                element.selector = selector;
                element.similar = similar;
                element.isIframe = isIframe;
                element.iframeIndex = iframeIndex;
    
                console.log(element);
            }
        });
    }

    const iframes = document.querySelectorAll("iframe");

    for (let i = 0; i < iframes.length; i++) {
        if (iframes[i].contentDocument !== null) {
            bind(iframes[i].contentDocument, true, i);
        }
    }
    bind(document, false, -1);

})();