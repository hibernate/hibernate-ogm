#!/usr/bin/env ruby
# encoding: UTF-8

########################################################################################################################
# The purpose of this tool is to handle several pre release tasks. These tasks are:
# - Update the data in readme.md
# - Update the changelog.txt (using JIRA's REST API to get the required information)
# - Print a release file template for the hibernate.org website
########################################################################################################################

require 'rubygems'
require 'bundler/setup'

require 'json'
require 'net/https'
require 'choice'
require 'git'

# REST URL used to retrieve all release versions of OGM - https://docs.atlassian.com/jira/REST/latest/#d2e4023
$jira_versions_url='https://hibernate.atlassian.net/rest/api/latest/project/OGM/versions'

# REST URL used for getting all issues of given release - see https://docs.atlassian.com/jira/REST/latest/#d2e2450
$jira_issues_url='https://hibernate.atlassian.net/rest/api/2/search/?jql=project%20%3D%20OGM%20AND%20fixVersion%20%3D%20#{release_version}%20ORDER%20BY%20issuetype%20ASC'

$now = Time.now

########################################################################################################################
# Defining the various command line arguments for this script
########################################################################################################################
Choice.options do
  header 'Application options:'

  separator 'Required:'

  option :release_version, :required => true do
    short '-v'
    long '--release-version=<version>'
    desc 'The OGM release version to process'
  end

  separator 'Optional:'

  option :website_release, :required => false do
    short '-w'
    long '--create-website-release-template'
    desc 'If specified the hibernate.org release file for the specified release will be created'
  end

  option :update_readme, :required => false do
    short '-r'
    long '--update-readme=<path to readme>'
    desc 'If specified the date in readme.md will be updated'
  end

  option :update_changelog, :required => false do
    short '-c'
    long '--update-change-log=<path to changelog>'
    desc 'If specified changelog.txt will be updated'
  end

  separator 'Common:'

  option :help do
    short '-h'
    long '--help'
    desc 'This scripts handles various OGM pre-release steps like changelog and readme updates.'
  end
end

########################################################################################################################
# Defining all required helper methods
########################################################################################################################
def get_json(url)
  uri = URI.parse(url)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true
  http.verify_mode = OpenSSL::SSL::VERIFY_NONE
  request = Net::HTTP::Get.new(uri.request_uri)
  response = http.request(request).body
  JSON.parse(response)
end

#######################################################################################################################
# We are dealing with something like this
#
# ...,
# {
#     "self": "https://hibernate.atlassian.net/rest/api/latest/version/18754",
#     "id": "18754",
#     "description": "Bugfixes for MongoDB, Neo4j and CouchDB backends",
#     "name": "4.1.2.Final",
#     "archived": false,
#     "released": true,
#     "releaseDate": "2015-02-27",
#     "userReleaseDate": "27/Feb/2015",
#     "projectId": 10160
# },
# ...
def get_release_info(release_version)
  jira_versions = get_json $jira_versions_url
  jira_version = jira_versions.find { |version| version['name'] == release_version }
  abort "ERROR: Version #{release_version} does not exist in JIRA" if jira_version.nil?
  abort "ERROR: Version #{release_version} is not yet released in JIRA" if !jira_version['released']
  jira_version
end

#######################################################################################################################
def create_website_release_file(jira_release_info)
  version = jira_release_info["name"]
  puts 'Run the following command in the hibernate.org repository to create the release announcement file:'
  puts ''
  puts '====='
  puts 'cat <<EOF > _data/projects/ogm/releases/' + jira_release_info['name'] + '.yml'
  puts 'version: ' + version
  puts 'version_family: ' + version.split('.')[0] + '.' + version.split('.')[1]
  puts 'date: ' + jira_release_info['releaseDate']
  puts 'stable: ' + (version.include?('Final') ? 'true' : 'false')
  puts 'announcement_url: <TBD>'
  puts 'summary: ' + (jira_release_info['description'] or '<TBD>')
  puts 'displayed: true'
  puts 'EOF'
  puts '====='
end

#######################################################################################################################
# Creates the required update for changelog.txt. It creates the following:
#
# <version> (<date>)
# -------------------------
#
# ** <issue-type-1>
#    * OGM-<key> - <component>  - <summary>
#    ...
#
# ** <issue-type-2>
#    * OGM-<key> - <component>  - <summary>
#    ...
#
def create_changelog_update(release_version)
  interpolated_url = eval '"' + $jira_issues_url + '"'
  jira_issues = get_json interpolated_url

  change_log_update = release_version + ' (' +  $now.strftime("%d-%m-%Y") + ")\n"
  change_log_update << "-------------------------\n"

  issue_type = ''
  jira_issues['issues'].each do |issue|
    current_issue_type = issue['fields']['issuetype']['name']
    if issue_type.empty? or !issue_type.eql? current_issue_type
      # create issue type entry
      issue_type = current_issue_type
      change_log_update << "\n** " << issue_type << "\n"
    end

    # issue key
    change_log_update << '    * ' << issue['key'] << ' '

    # components if set. As a simplification just take the first component
    if issue['fields']['components'].empty?
      change_log_update << ''.ljust(18)
    else
      components = '- ' << issue['fields']['components'][0]['name'].ljust(13) << ' - '
      change_log_update << components
    end

    # summary
    change_log_update << issue['fields']['summary'] << "\n"
  end
  change_log_update << "\n"
end

#######################################################################################################################
# Updates version and date in readme.md
def update_readme(readme_file_name, release_version)
  readme = File.read(readme_file_name)
  updated_readme = readme.gsub(/^Version:.*$/, "Version: #{release_version} - #{$now.strftime("%d %b %Y")}")

  # To write changes to the file, use:
  File.open(readme_file_name, "w") {|file| file.puts updated_readme }
end

#######################################################################################################################
def insert_lines(file_name, at_line, new_lines)
  open(file_name, 'r+') do |file|
    while (at_line -= 1) > 0          # read up to the line you want to write after
      file.readline
    end
    position = file.pos               # save your position in the file
    rest = file.read                  # save the rest of the file
    file.seek position                # go back to the old position
    file.puts [new_lines, rest]       # write new data & rest of file
  end
end

#######################################################################################################################
def git_commit(file_name, message)
  working_dir = File.dirname file_name
  git = Git.open(working_dir)
  git.add(file_name)
  git.commit(message)
end

########################################################################################################################
# Putting it all together
########################################################################################################################
release_version = Choice.choices[:release_version]
jira_release_info = get_release_info release_version

if Choice.choices[:website_release]
  create_website_release_file jira_release_info
end

readme_file_name = Choice.choices[:update_readme]
if !readme_file_name.nil? and !readme_file_name.empty?
  abort "ERROR: #{readme_file_name} is not a valid file" unless File.exist?(readme_file_name)
  update_readme(readme_file_name, release_version)
  git_commit(readme_file_name, "[Jenkins release job] readme.md updated by release build #{release_version}")
end

change_log_file_name = Choice.choices[:update_changelog]
if !change_log_file_name.nil? and !change_log_file_name.empty?
  abort "ERROR: #{change_log_file_name} is not a valid file" unless File.exist?(change_log_file_name)

  change_log_update = create_changelog_update release_version
  insert_lines(change_log_file_name, 4, change_log_update)
  git_commit(change_log_file_name, "[Jenkins release job] changelog.txt updated by release build #{release_version}")
end
