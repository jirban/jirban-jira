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


export class BoardData {
    public boardName:string;
    private _view:number;
    private _swimlane:string;
    private _issueTable:IssueTable;
    private _visibleColumns:boolean[] = [];

    public jiraUrl:string;

    private _projects:Projects;

    public blacklist:BlacklistData;

    public initialized = false;

    /** All the assignees */
    private _assignees:Indexed<Assignee>;
    /** All the priorities */
    private _priorities:Indexed<Priority>;
    /** All the issue types */
    private _issueTypes:Indexed<IssueType>;

    //Issue details
    private _issueDisplayDetails:IssueDisplayDetails = new IssueDisplayDetails();

    private _boardFilters:BoardFilters = new BoardFilters();

    private hideables:Hideable[] = [];

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
     * Called when changed data is pushed from the server
     * @param input the json containing the issue tables
     */
    messageFullRefresh(input:any) {
        this.internalDeserialize(input);
    }

    ///**
    // * Called when an issue is moved on the server's board
    // * @param input the json containing the details of the issue move
    // */
    //messageIssueMove(input:any) {
    //    console.log(input);
    //    this._issueTable.moveIssue(input.issueKey, input.toState, input.beforeIssue);
    //
    //}

    /**
     * Called when we receive a change set from the server
     *
     * @param input the json containing the details of the board change set
     */
    processChanges(input:any) {
        let changeSet:ChangeSet = new ChangeSet(input);
        if (changeSet.view != this.view) {
            if (changeSet.blacklistChanges) {
                if (!this.blacklist) {
                    this.blacklist = new BlacklistData();
                }
                this.blacklist.addChangeSet(changeSet);

                if (changeSet.blacklistIssues) {
                    this._issueTable.deleteIssues(changeSet.blacklistIssues);
                }
                /* TODO Deal with this
                    if (changeSet.blacklistClearedIssues) {
                        for (let issueKey of changeSet.blacklistClearedIssues) {
                            let keys:string[] = this.blacklist.issues;
                            for (let i:number = 0 ; i < keys.length ; i++) {
                                if (keys[i] === issueKey) {
                                    keys.splice(i, 1);
                                }
                            }
                        }
                    }
                */
            }


            //TODO Process the issue changes



            //Finally bump the view
            this._view = changeSet.view;
        }
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
        this.jiraUrl = RestUrlUtil.calculateJiraUrl();

        this.blacklist = input.blacklist ? BlacklistData.fromInput(input.blacklist) : null;

        this._projects = new ProjectDeserializer().deserialize(input);
        this._assignees = new AssigneeDeserializer().deserialize(input);
        this._priorities = new PriorityDeserializer().deserialize(input);
        this._issueTypes = new IssueTypeDeserializer().deserialize(input);

        if (first) {
            this._issueTable = new IssueTable(this, this._projects, this._boardFilters, this._swimlane, input);
        } else {
            this._issueTable.fullRefresh(this._projects, input);
        }
        //this.updateIssueTables();
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

    get priorities():Indexed<Priority> {
        return this._priorities;
    }

    get issueTypes():Indexed<IssueType> {
        return this._issueTypes;
    }

    get boardStates() : string[] {
        return this._projects.boardStates.array;
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
}



