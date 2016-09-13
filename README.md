# Jirban
Jirban is a Kanban board integrating with Jira. It is implemented as a Jira plugin with an Angular 2 client running in the browser. Webpack is used to bundle and minify the UI files. Its main purposes are:
* to address some of Jira Agile's short-comings when it comes to how it displays the boards. Effectively this means horizontally scrollable boards, with collapsible colums.
* to make setup of filters and swimlanes less static, minimising the need for configuration. Since we are using a 'fat' browser client, all changes to the view purely happen on the client side with no need for extra round trips to the server.
 
This file contains instructions for you to get up and running as a developer of Jirban. We also have a [User Guide](userguide).

## Set up of development environment
To develop Jirban locally you need to set up your development environment. You will need:
* the Atlassian SDK to build the plugin, and also to debug the Java server side classes
* NodeJS/Npm to download the Javascript libraries, and other tooling for working on the UI in isolation.

Since Angular 2 is used for the display logic, it is worth looking at the quickstart at https://angular.io.

### Atlassian SDK
The Atlassian SDK provides the APIs used for developing the plugin. It has tools for packaging the plugin to be deployed in the server, and also provides a development Jira instance where you can run/debug the plugin in a local Jira instance.

1. Download the Atlassian SDK and install it as outlined in https://developer.atlassian.com/docs/getting-started/set-up-the-atlassian-plugin-sdk-and-build-a-project.

You can use tgz based version from https://marketplace.atlassian.com/download/plugins/atlassian-plugin-sdk-tgz if you hit a problem on your platform.
Do not forget to adjust your PATH environment variable to include `${ATLASSIAN_SDK}/bin`. You can check proper setup by invoking `atlas-version` command.
Optionally you can modify `.m2/settings.xml` to include local repository `${ATLASSIAN_SDK}/repository` and online repository https://maven.atlassian.com/content/groups/public.

### NodeJS/npm
1. Install NodeJS as outlined at https://nodejs.org/en/download/package-manager
2. Make sure the output of `node --version` shows a 5 or 6 version.
3. Install the Node Package Manager (npm) by running `sudo npm install npm -g`)
4. Go to the `jirban-jira/webapp` folder, and run `npm install`. This is a one time step for freshly cloned projects, and will download all the javascript libraries needed for Angular 2 etc. It also needs to be performed if you add more libraries to the project. As we will see later, the maven plugin used to bundle the jar can also be used to update the packages.

## Building/running/debugging the project
The UI can be developed separately from the plugin itself, since it is a fat client interacting with the server via a REST API. So when modifying the UI files, you don't need to build, package and deploy in the server. The client logic is written in Typescript, and the UI steps are responsible for compiling the Typescript to Javascript. So depending on the focus of your current task, you can either do:
* just the UI steps (if working on purely display logic)
* or both the UI steps and the SDK steps if you are working on something involving the server. The SDK steps will package the jar containing the plugin.

When JIRA is started for the first time (e.g. running `atlas-run`) you should enable the Agile feature by acquiring evaluation license (it is valid for at least a month, and you can extend it), or buying the license (it is $10). You will need account on https://my.atlassian.com

### UI
Each of the following UI steps happen from the `jirban-jira/webapp` folder, and a separate terminal window is needed for each one.

1. Run `npm start`. This:
 * does an in-memory bundle using the webpack config files. The Typescript files to Javascript. Any errors from compiling will show up in this terminal window. Your files will be 'watched' so you can leave this running, and any changes will be recompiled (if you start seeing strange behaviour, restart this job).
 * starts the webpack dev server on port 3000, and launches a browser window where you can view your changes. The `rest/jirban/1.0` sub-folder contains some mock data files for running without a real jira instance so you can see how your changes affect the layout. As you make changes and they compile, the browser window refreshes automatically.
2. Run `npm test` which runs the client-side test suite. We don't have a huge amount of tests, but have attempted to at least test the most important/tricky areas. This step is only really necessary if you are modifying the UI, and not if your main purpose is to build the plugin for deployment in Jira.

### SDK
These commands happen from the root folder of the project (i.e where `pom.xml` is located). I normally use one window for running the server instance and another to package the project. Stopping and starting the server takes a lot of time.

1. Run `atlas-debug`. This builds the plugin, starts up the Jira instance, and deploys the plugin into it.
2. Once you change something, and want to deploy it into Jira, run `atlas-package` from another terminal window. This builds the plugin again, and deploys it into the running Jira instance we started in the previous step.
3. Raw `atlas-debug` or `atlas-package` just bundles any built web application files into the resulting Jirban plugin jar. We have two system properties to do more work.
3.1 `-Djirban.npm` - this installs a copy of npm and node so that they are usable from the maven plugin used to bundle the UI. If when pulling changes from git, any of the dependencies in `package.json` have changed, you should delete the `jirban-jira/webapp/node-modules` folder, and run `atlas-package -Djirban.npm` to get the fresh dependencies.
 3.2 `-Djirban.ui` - this runs the webpack bundler and refreshes the web application files to be used in the Jirban plugin jar. Since the webpack bundler takes some time to do its work, by default `atlas-package` does not bundle and refresh the web application files. This means that you can work on server-side code without the delay. If you are working on the web application files, and want to see the changes in your local Jira instance, run `atlas-package -Djirban.ui` to trigger the bundling and refreshing of the web application files on the Jirban plugin jar.

## Setting up projects in Jira
To be able to debug the Jirban plugin, you need to set up your SDK's Jira instance to contain some projects. I originally wanted to share a backup of my local Jira system, but that includes licence keys and things like that which are not a good idea to share. So you will need to do this manually. Use the exact project codes shown below, since we will be referencing those from the Jirban 

1. From Jira's 'Projects' menu, select 'Create Project'.
2. Select the 'Kanban software development' project type
3. Use 'Feature' as the project name, and 'FEAT' as the project code
4. Repeat steps 1-3 to create a project with the name 'Support' and the code 'SUP'
5. Repeat steps 1-3 to create a project with the name 'Upstream' and the code 'UP'
6. Create some issues in all three projects, and make some links from issues in 'Support' and 'Feature' to issues in the 'Upstream' project. Make sure that all available issue types and priorities are used for the issues (so that you have something to switch later when you run the board!).
7. In the Jira Agile boards for each project distribute the issues a bit throughout the states/columns
8. For more advanced development, set up some users and components in Jira and assign issues to those users/components.

## Configuring a Jirban board
1. Log in to your local Jira instance
2. From the 'Agile' menu, select 'Jirban Boards'
3. Click 'Config' in the top banner
4. Open http://localhost:2990/jira/rest/api/2/field in a browser and look for the field called 'Rank'. Enter its id (probably 10005) into the field on the bottom of the page and click the associated 'Save' button. Another option to get Rank field is invoking http://localhost:2990/jira/plugins/servlet/restbrowser#/resource/api-2-field.
5. Copy the text from `src/setup/board.json` into the text area on the page, and press 'Save'.

The following discusses the settings used for the board.

First we start off with a section defining the board's name, code, and the 'owning' project to display (this project's issues will always be displayed before the others).
```
{
  "name": "Test board",
  "code": "TST",
  "owning-project": "FEAT",
```
Next we list the board states in the order that they should be displayed. The name is what is visible to the user.
```
  "states": [
    {
```
The name is what is displayed to the user.
```
      "name": "Backlog",
```
This state is considered to be the backlog. It is hidden by default. There are other settings as well to categories several states within a header, and to be 'done' (Done states and their issues are not shown on the board to save bandwidth). The examples in `src/main/webapp/rest/jirban/1.0` should hopefully be enough to get you started.
```
      "backlog": true
    },
    {
      "name": "Selected for Development"
    },
    {
      "name": "In Progress"
    },
    {
      "name": "Done"
    }
  ],
```
We list all the priorities used for projects within the board. This is the order that they will show up on in the board's control panel.
```
  "priorities": [
    "Blocker",
    "Major",
    "Minor",
    "Trivial"
  ],
```
We list all the issue types used for projects within the board. This is the order that they will show up on in the board's control panel. If you leave out any priorities or issue types the health panel (accessible via the heart icon in the bottom right when viewing the board) will warn you of your configuration problem and list the affected issues. For components and assignees, since there can be so many of them within Jira, and they are not necessarily all known at the time we take a different approach and populate the control panel with the ones which are actually used by issues on the board. 
```
  "issue-types": [
    "Task",
    "Story",
    "Bug",
    "Epic"
  ],
```
Next we configure the main projects. A main project is a project whose issues will be shown on the board. Each project section is similar.
```
  "projects": {
```
The projects are indexed by their project code.
```
    "FEAT": {
```
You can enter a jql snippet to be part of the where clause to narrow down what is fetched. The default is to get all the issues for the project, apart from the ones in columns configured as 'done'.
```
      "query-filter": null,
```
The colour to use for issues in this project.
```
      "colour": "#4667CA",
```
Next we map the project's state names to the state names we set up in the board. The project state names are on the LHS of the name pairs. In this case all the names are exactly the same. If you forget to add a state, for which one of the configured projects is in, this will be pointed out in the health panel. If everything is set up properly, the health panel's heart icon will not be displayed.
```
      "state-links": {
        "Done": "Done",
        "Selected for Development": "Selected for Development",
        "In Progress": "In Progress",
        "Backlog": "Backlog"
      }
    },
    "SUP": {
      "query-filter": null,
      "colour": "#CA6746",
      "state-links": {
        "Done": "Done",
        "Selected for Development": "Selected for Development",
        "In Progress": "In Progress",
        "Backlog": "Backlog"
      }
    }
  },
```
Finally we have a section configuring projects whose issues we are interested in if they are linked to by any of the projects configured in the above section. If a main project issues links to any issues in these linked projects, the linked project issues are displayed in the bottom section of the main project issue's card.
```
  "linked-projects": {
    "UP": {
      "states": [
        "Backlog",
        "Selected for Development",
        "In Progress",
        "Done"
      ]
    }
  }
}
```
You can view a board's configuration by clicking on the name of the board in the list on the config page. From there you can edit it, if you wish.




