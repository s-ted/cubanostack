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
  ButtonInput
  ButtonToolbar
  Carousel
  CarouselItem
  Col
  CollapsibleNav
  Dropdown
  DropdownButton
  Dropdown.Menu
  Dropdown.Toggle
  FormControls
  FormControls.Static
  Glyphicon
  Grid
  Image
  Input
  Jumbotron
  Label
  ListGroup
  ListGroupItem
  MenuItem
  Modal
  ModalBody
  ModalFooter
  ModalHeader
  ModalTitle
  Modal.Body
  Modal.Dialog
  Modal.Footer
  Modal.Header
  Modal.Title
  Nav
  Navbar
  Navbar.Header
  Navbar.Toggle
  Navbar.Brand
  Navbar.Collapse
  NavBrand
  NavDropdown
  NavItem
  Overlay
  OverlayTrigger
  PageHeader
  PageItem
  Pager
  Pagination
  Panel
  PanelGroup
  Popover
  ProgressBar
  ResponsiveEmbed
  Row
  SafeAnchor
  SplitButton
  SplitButton.Toggle
  Tab
  Table
  Tabs
  Thumbnail
  Tooltip
  Well
  Collapse
  Fade)
