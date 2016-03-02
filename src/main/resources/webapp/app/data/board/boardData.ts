import {Assignee, AssigneeDeserializer} from './assignee';
import {Priority, PriorityDeserializer} from './priority';
import {IssueType, IssueTypeDeserializer} from './issueType';
import {BoardFilters, IssueDisplayDetails} from './boardFilters';
import {IssueData} from './issueData';

import {IMap} from '../../common/map';
import {Indexed} from '../../common/indexed';
import {isNumber} from "angular2/src/facade/lang";
import {Hideable} from "../../common/hide";
import {Projects} from "./project";
import {ProjectDeserializer} from "./project";
import {LinkedProject} from "./project";
import {BoardProject} from "./project";
import {IssueTable, SwimlaneData} from "./issueTable";
import {RestUrlUtil} from "../../common/RestUrlUtil";
import {BlacklistData} from "./blacklist";
import {ChangeSet} from "./change";
import {IssuesService} from "../../services/issuesService";
import {JiraComponent, ComponentDeserializer} from "./component";


export class BoardData {
    public boardName:string;
    private _view:number;
    private _swimlane:string;
    private _issueTable:IssueTable;
    private _visibleColumns:boolean[] = [];
    private _rankCustomFieldId:number;

    public jiraUrl:string;

    private _boardStates:Indexed<string>;

    private _projects:Projects;

    public blacklist:BlacklistData;

    public initialized = false;

    /** All the assignees */
    private _assignees:Indexed<Assignee>;
    /** All the components */
    private _components:Indexed<JiraComponent>;
    /** All the priorities */
    private _priorities:Indexed<Priority>;
    /** All the issue types */
    private _issueTypes:Indexed<IssueType>;

    //Issue details
    private _issueDisplayDetails:IssueDisplayDetails = new IssueDisplayDetails();

    private _boardFilters:BoardFilters = new BoardFilters();

    private hideables:Hideable[] = [];

    /** Flag to only recalculate the assignees in the control panel when they have been changed */
    private _hasNewAssignees:boolean = false;

    /** Flag to only recalculate the components in the control panel when they have been changed */
    private _hasNewComponents:boolean = false;

    /**
     * Called on loading the board the first time
     * @param input the json containing the issue tables
     */
    deserialize(boardId:number, input:any):BoardData {
        this.boardName = input.name;
        this.internalDeserialize(input, true);

        let arr:boolean[] = [];
        for (let i:number = 0; i < this.boardStates.length; i++) {
            arr.push(true);
        }
        this._visibleColumns = arr;

        this.initialized = true;
        return this;
    }

    /**
     * Called when we receive a change set from the server
     *
     * @param input the json containing the details of the board change set
     */
    processChanges(input:any) {
        if (!input.changes) {
            //This is a full refresh
            this.internalDeserialize(input);
        } else {
            let changeSet:ChangeSet = new ChangeSet(input);
            if (changeSet.view != this.view) {

                if (changeSet.addedAssignees) {
                    //Make sure that the added assignees are in alphabetical order for the control panel list
                    for (let assignee of changeSet.addedAssignees) {
                        this._assignees.add(assignee.key, assignee);
                    }
                    let assignees:Assignee[] = this._assignees.array;
                    assignees.sort((a1:Assignee, a2:Assignee) => {return a1.name.localeCompare(a2.name)});
                    this._assignees.reorder(assignees, (assignee:Assignee) => assignee.key);
                    this._hasNewAssignees = true;
                }
                if (changeSet.addedComponents) {
                    //Make sure that the added components are in alphabetical order for the control panel list
                    for (let component of changeSet.addedComponents) {
                        this._components.add(component.name, component);
                    }
                    let components:JiraComponent[] = this._components.array;
                    components.sort((c1:JiraComponent, c2:JiraComponent) => {return c1.name.localeCompare(c2.name)});
                    this._components.reorder(components, (component:JiraComponent) => component.name);
                    this._hasNewComponents = true;
                }


                if (changeSet.blacklistChanges) {
                    if (!this.blacklist) {
                        this.blacklist = new BlacklistData();
                    }
                    this.blacklist.addChangeSet(changeSet);
                }

                this._issueTable.processTableChanges(this, changeSet);


                //Finally bump the view
                this._view = changeSet.view;
            }
        }
    }

    getAndClearHasNewAssignees() : boolean {
        //TODO look into an Observable instead
        let ret:boolean = this._hasNewAssignees;
        if (ret) {
            this._hasNewAssignees = false;
        }
        return ret;
    }

    getAndClearHasNewComponents() : boolean {
        //TODO look into an Observable instead
        let ret:boolean = this._hasNewComponents;
        if (ret) {
            this._hasNewComponents = false;
        }
        return ret;
    }

    /**
     * Called when changes are made to the issue detail to display in the control panel
     * @param issueDisplayDetails
     */
    updateIssueDisplayDetails(issueDisplayDetails:IssueDisplayDetails) {
        this.issueDisplayDetails = issueDisplayDetails;
    }


    private internalDeserialize(input:any, first:boolean = false) {
        this._view = input.view;
        if (first) {
            this.jiraUrl = RestUrlUtil.calculateJiraUrl();
            this._rankCustomFieldId = input["rank-custom-field-id"];
        }

        this.blacklist = input.blacklist ? BlacklistData.fromInput(input.blacklist) : null;

        this._boardStates = new Indexed<string>();
        this._boardStates.indexArray(input.states, (entry)=>entry, (entry)=>entry);

        this._projects = new ProjectDeserializer(this._boardStates).deserialize(input);
        this._assignees = new AssigneeDeserializer().deserialize(input);
        this._components = new ComponentDeserializer().deserialize(input);
        this._priorities = new PriorityDeserializer().deserialize(input);
        this._issueTypes = new IssueTypeDeserializer().deserialize(input);

        if (first) {
            this._issueTable = new IssueTable(this, this._projects, this._boardFilters, this._swimlane, input);
        } else {
            this._issueTable.fullRefresh(this._projects, input);
        }
    }



    toggleColumnVisibility(stateIndex:number) {
        this._visibleColumns[stateIndex] = !this._visibleColumns[stateIndex];
    }

    toggleSwimlaneVisibility(swimlaneIndex:number) {
        this._issueTable.toggleSwimlaneVisibility(swimlaneIndex);
    }

    get view():number {
        return this._view;
    }

    get visibleColumns() : boolean[] {
        return this._visibleColumns
    }

    get issueTable():IssueData[][] {
        return this._issueTable.issueTable;
    }

    get swimlaneTable():SwimlaneData[] {
        return this._issueTable.swimlaneTable;
    }

    get totalIssuesByState() : number[] {
        return this._issueTable.totalIssuesByState;
    }

    get assignees():Indexed<Assignee> {
        return this._assignees;
    }

    get components():Indexed<JiraComponent> {
        return this._components;
    }

    get priorities():Indexed<Priority> {
        return this._priorities;
    }

    get issueTypes():Indexed<IssueType> {
        return this._issueTypes;
    }

    get boardStates() : string[] {
        return this._boardStates.array;
    }

    get indexedBoardStates():Indexed<string> {
        return this._boardStates;
    }

    get owner() : string {
        return this._projects.owner;
    }

    get linkedProjects() : IMap<LinkedProject> {
        return this._projects.linkedProjects;
    }

    get boardProjects() : Indexed<BoardProject> {
        return this._projects.boardProjects;
    }

    get boardProjectCodes() : string[] {
        return this._projects.boardProjectCodes;
    }

    get swimlane():string {
        return this._swimlane;
    }

    get issueDisplayDetails():IssueDisplayDetails {
        return this._issueDisplayDetails;
    }

    set swimlane(swimlane:string) {
        this._swimlane = swimlane;
        this._issueTable.swimlane = swimlane;
    }

    getIssue(issueKey:string) : IssueData {
        return this._issueTable.getIssue(issueKey);
    }

    updateIssueDetail(assignee:boolean, description:boolean, info:boolean, linked:boolean) {
        this._issueDisplayDetails = new IssueDisplayDetails(assignee, description, info, linked);
    }

    updateProjectFilter(filter:any) {
        this._boardFilters.setProjectFilter(filter, this._projects.boardProjectCodes);
        this._issueTable.filters = this._boardFilters;

    }

    updatePriorityFilter(filter:any) {
        this._boardFilters.setPriorityFilter(filter, this._priorities);
        this._issueTable.filters = this._boardFilters;
    }

    updateIssueTypeFilter(filter:any) {
        this._boardFilters.setIssueTypeFilter(filter, this._issueTypes);
        this._issueTable.filters = this._boardFilters;
    }

    updateAssigneeFilter(filter:any) {
        this._boardFilters.setAssigneeFilter(filter, this._assignees);
        this._issueTable.filters = this._boardFilters;
    }

    updateFilters(projectFilter:any, priorityFilter:any, issueTypeFilter:any, assigneeFilter:any, componentFilter:any) {
        this._boardFilters.setProjectFilter(projectFilter, this._projects.boardProjectCodes);
        this._boardFilters.setPriorityFilter(priorityFilter, this._priorities);
        this._boardFilters.setIssueTypeFilter(issueTypeFilter, this._issueTypes);
        this._boardFilters.setAssigneeFilter(assigneeFilter, this._assignees);
        this._boardFilters.setComponentFilter(componentFilter, this._components);
        this._issueTable.filters = this._boardFilters;
    }

    hideHideables() {
        for (let hideable of this.hideables) {
            hideable.hide();
        }
    }

    registerHideable(hideable:Hideable) {
        this.hideables.push(hideable);
    }

    /**
     * Checks whether a board state is valid for an issue
     * @param projectCode the project code
     * @param state the state to check
     */
    isValidStateForProject(projectCode:string, state:string):boolean {
        return this.boardProjects.forKey(projectCode).isValidState(state);
    }

    /**
     * Gets a list of the valid issues for a state, that an issue can be moved before/after. For example we don't allow
     * mixing of priority between issues from different projects. When swimlanes are used, we stay within the same swimlane,
     * or we would have to change the swimlane selector (e.g. assignee, project, priority, component etc.) in the
     * upstream jira issue.
     *
     * @param issueKey the key of the issue
     * @param toState the board state we are moving to
     * @returns {IssueData[]} the list of valid issues we can use for positioning
     */
    getValidMoveBeforeIssues(issueKey:string, toState:string) {
        let moveIssue:IssueData = this._issueTable.getIssue(issueKey);
        return this._projects.getValidMoveBeforeIssues(this._issueTable, this._swimlane, moveIssue, toState);
    }

    get rankCustomFieldId():number {
        return this._rankCustomFieldId;
    }
}



