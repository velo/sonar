/* global _:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

jQuery(function() {

  var AppState = Backbone.Model.extend({

    defaults: {
      canManageFilter: false,
      canBulkChange: false
    },


    url: function() {
      return baseUrl + '/api/issue_filters/page';
    }

  });



  var Issue = Backbone.Model.extend({

    url: function() {
      return baseUrl + '/api/issues/show?key=' + this.get('key');
    },


    parse: function(r) {
      return r.issue ? r.issue : r;
    }

  });



  var Issues = Backbone.Collection.extend({
    model: Issue,


    url: function() {
      return baseUrl + '/api/issues/search';
    },


    parse: function(r) {

      function find(source, key, keyField) {
        var searchDict = {};
        searchDict[keyField || 'key'] = key;
        return _.findWhere(source, searchDict) || key;
      }

      this.paging = r.paging;
      this.maxResultsReached = r.maxResultsReached;

      return r.issues.map(function(issue) {
        var component = find(r.components, issue.component),
            project = find(r.projects, issue.project),
            rule = find(r.rules, issue.rule);

        if (component) {
          _.extend(issue, {
            componentLongName: component.longName,
            componentQualifier: component.qualifier
          });
        }

        if (project) {
          _.extend(issue, {
            projectLongName: project.longName
          });
        }

        if (rule) {
          _.extend(issue, {
            ruleName: rule.name
          });
        }

        return issue;
      });

    }
  });



  var FavoriteFilter = Backbone.Model.extend({

    url: function() {
      return baseUrl + '/api/issue_filters/show/' + this.get('id');
    },


    parse: function(r) {
      return r.filter ? r.filter : r;
    }
  });



  var FavoriteFilters = Backbone.Collection.extend({
    model: FavoriteFilter,


    url: function() {
      return baseUrl + '/api/issue_filters/favorites';
    },


    parse: function(r) {
      return r.favoriteFilters;
    }
  });


  var Rule = Backbone.Model.extend({

    url: function() {
      return baseUrl + '/api/rules/show/?key=' + this.get('key');
    },


    parse: function(r) {
      return r.rule ? r.rule : r;
    }
  });



  var ActionPlans = Backbone.Collection.extend({

    url: function() {
      return baseUrl + '/api/action_plans/search';
    },


    parse: function(r) {
      return r.actionPlans;
    }

  });



  var IssueView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-template').html() || ''),
    tagName: 'li',


    events: {
      'click': 'showDetails'
    },


    modelEvents: {
      'change': 'render'
    },


    showDetails: function() {
      this.$el.parent().children().removeClass('active');
      this.$el.addClass('active');

      var app = this.options.app,
          detailView = new window.SS.IssueDetailView({
            model: this.model
          });

      if (this.model.get('line')) {
        jQuery.when(detailView.model.fetch(), this.fetchSource(detailView)).done(function() {
          app.detailsRegion.show(detailView);
        });
      } else {
        jQuery.when(detailView.model.fetch()).done(function() {
          app.detailsRegion.show(detailView);
        });
      }
    },


    fetchSource: function(view) {
      var from = this.model.get('line') - 10,
          to = this.model.get('line') + 30;

      return jQuery.ajax({
        type: 'GET',
        url: baseUrl + '/api/sources/show',
        data: {
          key: this.model.get('component'),
          from: from >= 0 ? from : 0,
          to: to,
          format: 'json'
        }
      }).done(function(r) {
            if (_.isObject(r) && r.source) {
              view.source = r.source;
            }
            if (_.isObject(r) && r.scm) {
              view.scm = r.scm;
            }
          });
    }
  });



  var NoIssuesView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#no-issues-template').html() || '')
  });



  var IssuesView = Backbone.Marionette.CollectionView.extend({
    tagName: 'ol',
    className: 'navigator-results-list',
    itemView: IssueView,
    emptyView: NoIssuesView,


    itemViewOptions: function() {
      return {
        issuesView: this,
        app: this.options.app
      };
    },


    onRender: function() {
      var $scrollEl = jQuery('.navigator-results'),
          scrollEl = $scrollEl.get(0),
          onScroll = function() {
            if (scrollEl.offsetHeight + scrollEl.scrollTop >= scrollEl.scrollHeight) {
              window.SS.IssuesNavigatorApp.fetchNextPage();
            }
          },
          throttledScroll = _.throttle(onScroll, 300);
      $scrollEl.off('scroll').on('scroll', throttledScroll);
    },


    close: function() {
      var scrollEl = jQuery('.navigator-results');
      scrollEl.off('scroll');
      Backbone.Marionette.CollectionView.prototype.close.call(this);
    }

  });



  var IssuesActionsView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issues-actions-template').html() || ''),


    collectionEvents: {
      'sync': 'render'
    },


    events: {
      'click .navigator-actions-order': 'toggleOrderChoices',
      'click .navigator-actions-order-choices': 'sort'
    },


    ui: {
      orderChoices: '.navigator-actions-order-choices'
    },


    onRender: function() {
      if (!this.collection.sorting.sortText) {
        this.collection.sorting.sortText = this.$('[data-sort=' + this.collection.sorting.sort + ']:first').text();
        this.render();
        return;
      }

      this.$el.toggle(this.collection.length > 0);
      this.$('.open-modal').modal();
    },


    toggleOrderChoices: function(e) {
      e.stopPropagation();
      this.ui.orderChoices.toggleClass('open');
      if (this.ui.orderChoices.is('.open')) {
        var that = this;
        jQuery('body').on('click.issues_actions', function() {
          that.ui.orderChoices.removeClass('open');
        });
      }
    },


    sort: function(e) {
      e.stopPropagation();
      this.ui.orderChoices.removeClass('open');
      jQuery('body').off('click.issues_actions');
      var el = jQuery(e.target),
          sort = el.data('sort'),
          asc = el.data('asc');
      this.collection.sorting = {
        sort: sort,
        sortText: el.text(),
        asc: asc
      };
      this.options.app.fetchFirstPage();
    },


    serializeData: function() {
      var data = Backbone.Marionette.ItemView.prototype.serializeData.apply(this, arguments);
      return _.extend(data || {}, {
        paging: this.collection.paging,
        sorting: this.collection.sorting,
        maxResultsReached: this.collection.maxResultsReached,
        appState: window.SS.appState.toJSON(),
        query: (Backbone.history.fragment || '').replace(/\|/g, '&')
      });
    }
  });



  var IssuesFilterBarView = window.SS.FilterBarView.extend({

    collectionEvents: {
      'change:enabled': 'changeEnabled'
    },


    events: {
      'click .navigator-filter-submit': 'search'
    },


    getQuery: function() {
      var query = {};
      this.collection.each(function(filter) {
        _.extend(query, filter.view.formatValue());
      });
      return query;
    },


    search: function() {
      this.options.app.state.set({
        query: this.options.app.getQuery(),
        search: true
      });
      this.options.app.fetchFirstPage();
    },


    fetchNextPage: function() {
      this.options.app.fetchNextPage();
    }

  });



  var IssuesHeaderView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issues-header-template').html() || ''),


    modelEvents: {
      'change': 'render'
    },


    events: {
      'click #issues-new-search': 'newSearch',
      'click #issues-filter-save-as': 'saveAs',
      'click #issues-filter-save': 'save',
      'click #issues-filter-copy': 'copy',
      'click #issues-filter-edit': 'edit'
    },


    initialize: function(options) {
      Backbone.Marionette.ItemView.prototype.initialize.apply(this, arguments);
      this.listenTo(options.app.state, 'change', this.render);
    },


    newSearch: function() {
      this.model.clear();
      this.options.app.router.navigate('statuses=OPEN,REOPENED', { trigger: true });
    },


    saveAs: function() {
      var url = baseUrl + '/issues/save_as_form?' + (Backbone.history.fragment || '').replace(/\|/g, '&');
      openModalWindow(url, {});
    },


    save: function() {
      var that = this;
          url = baseUrl + '/issues/save/' + this.model.id + '?' + (Backbone.history.fragment || '').replace(/\|/g, '&');
      jQuery.ajax({
        type: 'POST',
        url: url
      }).done(function() {
            that.options.app.state.set('search', false);
          });
    },


    copy: function() {
      var url = baseUrl + '/issues/copy_form/' + this.model.id;
      openModalWindow(url, {});
    },


    edit: function() {
      var url = baseUrl + '/issues/edit_form/' + this.model.id;
      openModalWindow(url, {});
    },


    serializeData: function() {
      return _.extend({
        canSave: this.model.id && this.options.app.state.get('search'),
        appState: window.SS.appState.toJSON()
      }, this.model.toJSON());
    }

  });



  var IssueDetailCommentFormView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-detail-comment-form-template').html() || ''),


    ui: {
      textarea: '#issue-comment-text',
      cancelButton: '#issue-comment-cancel',
      submitButton: '#issue-comment-submit'
    },


    events: {
      'keyup #issue-comment-text': 'toggleSubmit',
      'click #issue-comment-cancel': 'cancel',
      'click #issue-comment-submit': 'submit'
    },


    toggleSubmit: function() {
      this.ui.submitButton.prop('disabled', this.ui.textarea.val().length === 0);
    },


    cancel: function() {
      this.options.detailView.updateAfterAction(false);
    },


    submit: function() {
      var that = this,
          text = this.ui.textarea.val(),
          update = this.model && this.model.has('key'),
          url = baseUrl + '/api/issues/' + (update ? 'edit_comment' : 'add_comment'),
          data = { text: text };

      if (update) {
        data.key = this.model.get('key');
      } else {
        data.issue = this.options.issue.get('key');
      }

      jQuery.ajax({
        type: 'POST',
        url: url,
        data: data
      }).done(function() {
            that.options.detailView.updateAfterAction(true);
          });
    }
  });



  var IssueDetailSetSeverityFormView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-detail-set-severity-form-template').html() || ''),


    ui: {
      select: '#issue-set-severity-select'
    },


    events: {
      'click #issue-set-severity-cancel': 'cancel',
      'click #issue-set-severity-submit': 'submit'
    },


    onRender: function() {
      this.ui.select.select2({
        minimumResultsForSearch: 100
      });
    },


    cancel: function() {
      this.options.detailView.updateAfterAction(false);
    },


    submit: function() {
      var that = this;

      jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/set_severity',
        data: {
          issue: this.options.issue.get('key'),
          severity: this.ui.select.val()
        }
      }).done(function() {
            that.options.detailView.updateAfterAction(true);
          });
    }
  });



  var IssueDetailAssignFormView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-detail-assign-form-template').html() || ''),


    ui: {
      select: '#issue-assignee-select'
    },


    events: {
      'click #issue-assign-cancel': 'cancel',
      'click #issue-assign-submit': 'submit'
    },


    onRender: function() {
      var currentUser = window.SS.currentUser,
          assignee = this.options.issue.get('assignee'),
          additionalChoices = [];

      if (!assignee || currentUser !== assignee) {
        additionalChoices.push({
          id: currentUser,
          text: window.SS.phrases.assignedToMe
        });
      }

      if (!!assignee) {
        additionalChoices.push({
          id: '',
          text: window.SS.phrases.unassigned
        });
      }

      var select2Options = {
        allowClear: false,
        width: '250px',
        formatNoMatches: function() { return window.SS.phrases.select2.noMatches; },
        formatSearching: function() { return window.SS.phrases.select2.searching; },
        formatInputTooShort: function() { return window.SS.phrases.select2.tooShort; }
      };

      if (additionalChoices.length > 0) {
        select2Options.minimumInputLength = 0;
        select2Options.query = function(query) {
          if (query.term.length == 0) {
            query.callback({ results: additionalChoices });
          } else if (query.term.length >= 2) {
            jQuery.ajax({
              url: baseUrl + '/api/users/search?f=s2',
              data: { s: query.term },
              dataType: 'jsonp'
            }).done(function(data) {
                  query.callback(data);
                });
          }
        }
      } else {
        select2Options.minimumInputLength = 2;
        select2Options.ajax = {
          quietMillis: 300,
          url: baseUrl + '/api/users/search?f=s2',
          data: function (term, page) {
            return {s: term, p: page}
          },
          results: function (data) {
            return { more: data.more, results: data.results }
          }
        };
      }

      this.ui.select.select2(select2Options).select2('open');
    },


    cancel: function() {
      this.options.detailView.updateAfterAction(false);
    },


    submit: function() {
      var that = this;

      jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/assign',
        data: {
          issue: this.options.issue.get('key'),
          assignee: this.ui.select.val()
        }
      }).done(function() {
            that.options.detailView.updateAfterAction(true);
          });
    }
  });



  var IssueDetailPlanFormView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-detail-plan-form-template').html() || ''),


    collectionEvents: {
      'reset': 'render'
    },


    ui: {
      select: '#issue-detail-plan-select'
    },


    events: {
      'click #issue-plan-cancel': 'cancel',
      'click #issue-plan-submit': 'submit'
    },


    onRender: function() {
      this.ui.select.select2({
        width: '250px',
        minimumResultsForSearch: 100
      });

      this.$('.error a')
          .prop('href', baseUrl + '/action_plans/index/' + this.options.issue.get('project'));
    },


    cancel: function() {
      this.options.detailView.updateAfterAction(false);
    },


    submit: function() {
      var that = this;

      jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/plan',
        data: {
          issue: this.options.issue.get('key'),
          plan: this.ui.select.val()
        }
      }).done(function() {
            that.options.detailView.updateAfterAction(true);
          });
    },


    serializeData: function() {
      return {
        items: this.collection.toJSON(),
        issue: this.options.issue.toJSON()
      }
    }
  });



  var IssueDetailRuleView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-detail-rule-template').html() || ''),
    className: 'rule-desc',
    modelEvents: { 'change': 'render' }
  });



  var IssueDetailView = Backbone.Marionette.Layout.extend({
    template: Handlebars.compile(jQuery('#issue-detail-template').html() || ''),


    regions: {
      formRegion: '.code-issue-form',
      ruleRegion: '#tab-issue-rule'
    },


    events: {
      'click [href=#tab-issue-rule]': 'fetchRule',

      'click #issue-comment': 'comment',
      'click .issue-comment-edit': 'editComment',
      'click .issue-comment-delete': 'deleteComment',
      'click .issue-transition': 'transition',
      'click #issue-set-severity': 'setSeverity',
      'click #issue-assign': 'assign',
      'click #issue-assign-to-me': 'assignToMe',
      'click #issue-plan': 'plan'
    },


    modelEvents: {
      'change': 'render'
    },


    onRender: function() {
      this.$('.code-issue-details').tabs();
      this.$('.code-issue-form').hide();
      this.rule = new Rule({ key: this.model.get('rule') });
      this.ruleRegion.show(new IssueDetailRuleView({ model: this.rule }));
    },


    onClose: function() {
      if (this.ruleRegion) {
        this.ruleRegion.reset();
      }
    },


    resetIssue: function(options) {
      var key = this.model.get('key');
      this.model.clear({ silent: true });
      this.model.set({ key: key }, { silent: true });
      return this.model.fetch(options);
    },


    fetchRule: function() {
      this.rule.fetch();
    },


    showActionView: function(view) {
      this.$('.code-issue-actions').hide();
      this.$('.code-issue-form').show();
      this.formRegion.show(view);
    },


    updateAfterAction: function(fetch) {
      var that = this;

      if (fetch) {
        jQuery.when(this.resetIssue()).done(function() {
          that.formRegion.reset();
          that.$('.code-issue-actions').show();
          that.$('.code-issue-form').hide();
          that.$('[data-comment-key]').show();
        });
      } else {
        that.formRegion.reset();
        that.$('.code-issue-actions').show();
        that.$('.code-issue-form').hide();
        that.$('[data-comment-key]').show();
      }
    },


    comment: function() {
      var commentFormView = new IssueDetailCommentFormView({
        issue: this.model,
        detailView: this
      });
      this.showActionView(commentFormView);
    },


    editComment: function(e) {
      var commentEl = jQuery(e.target).closest('[data-comment-key]'),
          commentKey = commentEl.data('comment-key'),
          comment = _.findWhere(this.model.get('comments'), { key: commentKey });

      commentEl.hide();

      var commentFormView = new IssueDetailCommentFormView({
        model: new Backbone.Model(comment),
        issue: this.model,
        detailView: this
      });
      this.showActionView(commentFormView);
    },


    deleteComment: function(e) {
      var that = this,
          commentKey = jQuery(e.target).closest('[data-comment-key]').data('comment-key'),
          confirmMsg = jQuery(e.target).data('confirm-msg');

      if (confirm(confirmMsg)) {
        jQuery.ajax({
          type: "POST",
          url: baseUrl + "/issue/delete_comment?id=" + commentKey
        }).done(function() {
              that.updateAfterAction(true);
            });
      }
    },


    transition: function(e) {
      var that = this;
      jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/do_transition',
        data: {
          issue: this.model.get('key'),
          transition: jQuery(e.target).data('transition')
        }
      }).done(function() {
            that.resetIssue();
          });
    },


    setSeverity: function() {
      var setSeverityFormView = new IssueDetailSetSeverityFormView({
        issue: this.model,
        detailView: this
      });
      this.showActionView(setSeverityFormView);
    },


    assign: function() {
      var assignFormView = new IssueDetailAssignFormView({
        issue: this.model,
        detailView: this
      });
      this.showActionView(assignFormView);
    },


    assignToMe: function() {
      var that = this;
      jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/assign',
        data: {
          issue: this.model.get('key'),
          assignee: window.SS.currentUser
        }
      }).done(function() {
            that.resetIssue();
          });
    },


    plan: function() {
      var that = this,
          actionPlans = new ActionPlans(),
          planFormView = new IssueDetailPlanFormView({
            collection: actionPlans,
            issue: this.model,
            detailView: this
          });

      actionPlans.fetch({
        reset: true,
        data: { project: this.model.get('project') },
        success: function() {
          that.showActionView(planFormView);
        }
      });
    },

    action: function(e) {
      var that = this,
          actionKey = jQuery(e.target).data('action');
      jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/do_action',
        data: {
          issue: this.model.get('key'),
          actionKey: actionKey
        }
      }).done(function() {
            that.resetIssue();
          });
    },


    serializeData: function() {
      return _.extend({
        source: this.source,
        scm: this.scm
      }, this.model.toJSON());
    }

  });



  var IssuesDetailsFavoriteFilterView = window.SS.DetailsFavoriteFilterView.extend({
    template: Handlebars.compile(jQuery('#issues-details-favorite-filter-template').html() || ''),


    applyFavorite: function(e) {
      var id = $j(e.target).data('id'),
          filter = new window.SS.FavoriteFilter({ id: id }),
          app = this.options.filterView.options.app;

      filter.fetch({
        success: function() {
          app.state.set('search', false);
          app.favoriteFilter.set(filter.toJSON());
        }
      });

      this.options.filterView.hideDetails();
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        items: this.model.get('choices')
      });
    }
  });



  var IssuesFavoriteFilterView = window.SS.FavoriteFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: IssuesDetailsFavoriteFilterView
      });

      this.listenTo(window.SS.appState, 'change:favorites', this.updateFavorites);
    },


    updateFavorites: function() {
      this.model.set('choices', window.SS.appState.get('favorites'));
      this.render();
    }
  });



  var IssuesRouter = Backbone.Router.extend({

    routes: {
      '': 'emptyQuery',
      ':query': 'index'
    },


    initialize: function(options) {
      this.app = options.app;
    },


    parseQuery: function(query, separator) {
      return (query || '').split(separator || '|').map(function(t) {
        var tokens = t.split('=');
        return {
          key: tokens[0],
          value: decodeURIComponent(tokens[1])
        }
      });
    },


    emptyQuery: function() {
      this.navigate('statuses=OPEN,REOPENED', { trigger: true });
    },


    index: function(query) {
      var params = this.parseQuery(query);

      var idObj = _.findWhere(params, { key: 'id' });
      if (idObj) {
        var that = this,
            f = this.app.favoriteFilter;
        this.app.canSave = false;
        f.set('id', idObj.value);
        f.fetch({
          success: function() {
            params = _.extend({}, that.parseQuery(f.get('query')), params);
            that.loadResults(params);
          }
        });
      } else {
        this.loadResults(params);
      }
    },


    loadResults: function(params) {
      this.app.filterBarView.restoreFromQuery(params);
      this.app.restoreSorting(params);
      this.app.fetchFirstPage();
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    AppState: AppState,
    Issue: Issue,
    Issues: Issues,
    FavoriteFilter: FavoriteFilter,
    FavoriteFilters: FavoriteFilters,
    IssueView: IssueView,
    IssuesView: IssuesView,
    IssuesActionsView: IssuesActionsView,
    IssuesFilterBarView: IssuesFilterBarView,
    IssuesHeaderView: IssuesHeaderView,
    IssuesFavoriteFilterView: IssuesFavoriteFilterView,
    IssueDetailView: IssueDetailView,
    IssuesRouter: IssuesRouter
  });

});
