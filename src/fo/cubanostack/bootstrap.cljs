;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns cubanostack.bootstrap
  (:require
    [quiescent.factory :as f]
    cljsjs.react-bootstrap))

(f/def-factories js/ReactBootstrap
  Accordion
  Alert
  Badge
  Breadcrumb
  BreadcrumbItem
  Button
  ButtonGroup
  ButtonToolbar
  Carousel
  Carousel.Caption
  Carousel.Item
  Checkbox
  Clearfix
  Col
  Collapse
  ControlLabel
  Dropdown
  DropdownButton
  Dropdown.Menu
  Dropdown.Toggle
  Fade
  Form
  FormControl
  FormControl.Feedback
  FormControl.Static
  FormGroup
  Glyphicon
  Grid
  HelpBlock
  Image
  InputGroup
  InputGroup.Addon
  InputGroup.Button
  Jumbotron
  Label
  ListGroup
  ListGroupItem
  Media
  Media.Body
  Media.Heading
  Media.Left
  Media.List
  Media.ListItem
  Media.Right
  MenuItem
  Modal
  Modal.Body
  Modal.Dialog
  Modal.Footer
  Modal.Header
  Modal.Title
  Nav
  NavDropdown
  NavItem
  Navbar
  Navbar.Brand
  Navbar.Collapse
  Navbar.Header
  Navbar.Toggle
  Overlay
  OverlayTrigger
  PageHeader
  PageItem
  Pager
  Pager.Item
  Pagination
  Panel
  PanelGroup
  Popover
  ProgressBar
  Radio
  ResponsiveEmbed
  Row
  SafeAnchor
  SplitButton
  SplitButton.Toggle
  Tab
  Tab.Container
  Tab.Content
  Tab.Pane
  Table
  Tabs
  Thumbnail
  Tooltip
  Well)
