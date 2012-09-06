
var RDFaDOM = {

  init: function (doc) {
    doc = doc || window.document;
    var ctx = this.initialContext();
    this.walk(doc.documentElement, ctx);
    this.observer = this.setupObserver(doc);
    return new this.DocumentAccessor(doc);
  },

  walk: function (el, ctx) {
    ctx = this.subContext(el, ctx);
    if (el.attributes.length)
      this.expandInElement(ctx, el);
    for (var l=el.childNodes, it=null, i=0; it=l[i++];) {
      if (it.nodeType === 1)
        this.walk(it, ctx);
    }
  },

  initialContext: function () {
    return new RDFaParser.Context(null, 'html', null, null, RDFaParser.contexts.html);
  },

  subContext: function (el, ctx) {
    var data = new RDFaParser.ElementData(el, ctx, null);
    return data.context;
  },

  attrs: ['property', 'typeof', 'rel', 'rev'],
  refs: ['about', 'resource'],

  expandInElement: function (ctx, el) {
    for (var l=this.attrs, it=null, i=0; it=l[i++];) {
      this.expandTermOrCurie(ctx, el, it);
    }
    for (var l=this.refs, it=null, i=0; it=l[i++];) {
      this.expandRef(ctx, el, it);
    }
    // TODO: only resolveRef (and set resource proxy)
    this.expandRef(ctx, el, 'href');
    this.expandRef(ctx, el, 'src');
  },

  expandTermOrCurie: function (ctx, el, attr) {
    var expand = function (v) { return ctx.expandTermOrCurieOrIRI(v); };
    return this.runExpand(expand, el, attr);
  },

  expandRef: function (ctx, el, attr) {
    var expand = function (v) { return ctx.expandCurieOrIRI(v); };
    return this.runExpand(expand, el, attr, 'resource');
  },

  runExpand: function (expand, el, attr, destAttr) {
    var v = el.getAttribute(attr);
    if (!v) { return; }
    var iris = v.split(/\s+/).map(function (v) { return expand(v) || v; });
    destAttr = 'data-rdfa-' + (destAttr || attr);
    el.setAttribute(destAttr, iris.join(' '));
  },

  //getElementsBySubjectAndProperty...

  setupObserver: function (doc) {
    if (typeof MutationObserver === 'undefined') {
      if (typeof WebKitMutationObserver === 'function') {
        MutationObserver = WebKitMutationObserver;
      } else {
        return;
      }
    }
    // TODO: all attrs; re-compute context where needed and getInheritedContext...
    var self = this;
    var opts = {attributes: true, subtree: true, characterData: false};
    var observer = new MutationObserver(function(mutations) {
      mutations.forEach(function(mutation) {
        if (mutation.type !== 'attributes')
          return
        // avoid listening to own change
        observer.disconnect();
        var attr = mutation.attributeName,
          target = mutation.target,
          ctx = null;
        // TODO: if typeof or prefix: set context and recompute subtree
        if (self.attrs.indexOf(attr) > -1) {
          var ctx = RDFaDOM.initialContext();
          RDFaDOM.expandTermOrCurie(ctx, target, attr);
        } else if (self.refs.indexOf(attr) > -1) {
          ctx = ctx || RDFaDOM.initialContext();
          RDFaDOM.expandRef(ctx, target, attr);
        }
        // re-observe after disconnect
        observer.observe(doc, opts);
      });
    });
    observer.observe(doc, opts);
    return observer;
  }

};

RDFaDOM.DocumentAccessor = function (doc) {
  this.doc = doc;
};
RDFaDOM.DocumentAccessor.prototype = {

  getElementsByProperty: function (p, o) {
    if (o !== undefined)
      return this.getElementsByLink(p, o);
    return this.doc.querySelectorAll(
      "[data-rdfa-property~='"+ p +"'], " +
        "[data-rdfa-rel~='"+ p +"']");
  },

  getElementsByType: function (t) {
    return this.doc.querySelectorAll("[data-rdfa-typeof~='"+ t +"']");
  },

  getElementsBySubject: function (s) {
    return this.doc.querySelectorAll(
      "[data-rdfa-about~='"+ s +"'], " +
        "[data-rdfa-resource~='"+ s +"'][typeof], " +
          "[data-rdfa-resource~='"+ s +"']:not([property])");
  },

  getElementsByLink: function (p, o) {
    return this.doc.querySelectorAll(
      "[data-rdfa-property~='"+ p +"'][data-rdfa-resource~='"+ o +"']," +
        "[data-rdfa-rel~='"+ p +"'][data-rdfa-resource~='"+ o +"']");
  }

};

if (typeof module !== 'undefined') {
  module.exports = RDFaDOM;
}
