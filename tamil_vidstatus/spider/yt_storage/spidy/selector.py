import re


class Selector:
    def __init__(self, soup):
        self.soup = soup
        self.is_list = type(self.soup) is list
        self.size = len(self.soup) if self.is_list else 1


    def __repr__(self):
        return str(self.soup)

    def __iter__(self):
        return iter([Selector(s) for s in self.soup]) if self.is_list else iter([self.soup])

    def __getitem__(self, i):
        if self.is_list:
            return Selector(self.soup[i])
        elif i == 0:
            return self.soup

    def _xpath(self, element):
        components = []
        child = element if element.name else element.parent
        for parent in child.parents:  # type: bs4.element.Tag
            siblings = parent.find_all(child.name, recursive=False)
            components.append(
                child.name if 1 == len(siblings) else '%s[%d]' % (
                    child.name,
                    next(i for i, s in enumerate(siblings, 1) if s is child)
                    )
                )
            child = parent
        components.reverse()
        return '/%s' % '/'.join(components)

    def xpath(self, sel):
        if self.is_list:
            paths = []
            for s in self.soup:
                paths.append(self._xpath(s))
            return paths
        else:
            return self._xpath(self.soup)

    def regex(self, reg):
        r = r"({})".format(reg)
        res = []
        if self.is_list:
            for s in self.soup:
                txt = s.text
                m = re.findall(r, txt)
                for f in m: res.append(f)
        else:
            txt = self.soup.text
            m = re.findall(r, txt)
            for f in m: res.append(f)

        return res

    def dispose(self):
        if self.is_list:
            for s in self.soup:
                s.decompose()
        else:
            self.soup.decompose()

    def text(self):
        if self.is_list:
            txts = []
            for s in self.soup:
                txts.append(s.text)
            return txts
        else:
            return self.soup.text

    def attr(self, k, v=None):
        if self.is_list:
            attrs = []
            for s in self.soup:
                if v is not None: s[k] = v
                attrs.append(s[k])
            return attrs
        else:
            if v is not None: self.soup[k] = v
            return self.soup[k]

    def css(self, selector):
        els = []
        if type(self.soup) is list:
            for s in self.soup:
                sels = s.select(selector)
                for sel in sels:
                    els.append(sel)
        else:
            sels = self.soup.select(selector)
            for sel in sels:
                els.append(sel)

        if len(els) == 1:
            return Selector(els[0])
        else:
            return Selector(els)
