import { useState, useEffect, useCallback } from "react";
import { Folder, X, ChevronRight } from "lucide-react";
import { FileTree } from "./FileTree";
import { CodeEditor } from "./CodeEditor";
import { FileTabs } from "./FileTabs";
import { Button } from "@/components/ui/button";
import { api, FileNode, OPEN_TABS_KEY, ACTIVE_TAB_KEY } from "@/lib/api";
import { cn } from "@/lib/utils";

interface CodePanelProps {
  projectId: string;
  updatedFiles: Map<string, string>;
}

// Helper to find a file by path in the tree
function findFileInTree(files: FileNode[], targetPath: string): boolean {
  for (const node of files) {
    if (node.path === targetPath) return true;
    if (node.children && findFileInTree(node.children, targetPath)) return true;
  }
  return false;
}

// Storage key helpers
const getTabsKey = (projectId: string) => `${OPEN_TABS_KEY}_${projectId}`;
const getActiveTabKey = (projectId: string) => `${ACTIVE_TAB_KEY}_${projectId}`;

export function CodePanel({ projectId, updatedFiles }: CodePanelProps) {
  const [files, setFiles] = useState<FileNode[]>([]);
  const [openTabs, setOpenTabs] = useState<string[]>([]);
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const [fileContent, setFileContent] = useState<string>("");
  const [isLoadingTree, setIsLoadingTree] = useState(true);
  const [isLoadingFile, setIsLoadingFile] = useState(false);
  const [isMobileFileTreeOpen, setIsMobileFileTreeOpen] = useState(false);

  // Load tabs from localStorage
  useEffect(() => {
    const savedTabs = localStorage.getItem(getTabsKey(projectId));
    const savedActiveTab = localStorage.getItem(getActiveTabKey(projectId));
    
    if (savedTabs) {
      try {
        const tabs = JSON.parse(savedTabs);
        if (Array.isArray(tabs) && tabs.length > 0) {
          setOpenTabs(tabs);
          setActiveTab(savedActiveTab || tabs[0]);
          return;
        }
      } catch (e) {
        console.error("Failed to parse saved tabs:", e);
      }
    }
  }, [projectId]);

  // Save tabs to localStorage whenever they change
  useEffect(() => {
    if (openTabs.length > 0) {
      localStorage.setItem(getTabsKey(projectId), JSON.stringify(openTabs));
    } else {
      localStorage.removeItem(getTabsKey(projectId));
    }
  }, [openTabs, projectId]);

  // Save active tab to localStorage
  useEffect(() => {
    if (activeTab) {
      localStorage.setItem(getActiveTabKey(projectId), activeTab);
    } else {
      localStorage.removeItem(getActiveTabKey(projectId));
    }
  }, [activeTab, projectId]);

  // Load file tree
  useEffect(() => {
    const loadFiles = async () => {
      setIsLoadingTree(true);
      try {
        const fileTree = await api.getFiles(projectId);
        setFiles(fileTree);
        
        // If no tabs are open, default to pages/Index.tsx
        if (openTabs.length === 0) {
          const defaultPaths = ["src/pages/Index.tsx", "pages/Index.tsx"];
          for (const defaultPath of defaultPaths) {
            if (findFileInTree(fileTree, defaultPath)) {
              setOpenTabs([defaultPath]);
              setActiveTab(defaultPath);
              break;
            }
          }
        }
      } catch (error) {
        console.error("Failed to load files:", error);
      } finally {
        setIsLoadingTree(false);
      }
    };

    loadFiles();
  }, [projectId]);

  // Load file content when active tab changes
  useEffect(() => {
    if (!activeTab) {
      setFileContent("");
      return;
    }

    // Check if we have an updated version from streaming
    if (updatedFiles.has(activeTab)) {
      setFileContent(updatedFiles.get(activeTab)!);
      return;
    }

    const loadContent = async () => {
      setIsLoadingFile(true);
      try {
        const content = await api.getFileContent(projectId, activeTab);
        setFileContent(content);
      } catch (error) {
        console.error("Failed to load file:", error);
        setFileContent("// Error loading file");
      } finally {
        setIsLoadingFile(false);
      }
    };

    loadContent();
  }, [projectId, activeTab, updatedFiles]);

  // Update content when streaming updates arrive for active file
  useEffect(() => {
    if (activeTab && updatedFiles.has(activeTab)) {
      setFileContent(updatedFiles.get(activeTab)!);
    }
  }, [activeTab, updatedFiles]);

  const handleSelectFile = useCallback((path: string) => {
    // Add to tabs if not already open
    if (!openTabs.includes(path)) {
      setOpenTabs((prev) => [...prev, path]);
    }
    setActiveTab(path);
    setIsMobileFileTreeOpen(false);
  }, [openTabs]);

  const handleCloseTab = useCallback((path: string) => {
    setOpenTabs((prev) => {
      const newTabs = prev.filter((t) => t !== path);
      
      // If closing active tab, switch to another tab
      if (activeTab === path) {
        const closingIndex = prev.indexOf(path);
        const newActiveIndex = Math.min(closingIndex, newTabs.length - 1);
        setActiveTab(newTabs[newActiveIndex] || null);
      }
      
      return newTabs;
    });
  }, [activeTab]);

  const handleSelectTab = useCallback((path: string) => {
    setActiveTab(path);
  }, []);

  return (
    <div className="flex h-full relative overflow-hidden">
      {/* 1. Desktop File Tree (hidden on mobile, visible on md+) */}
      <div className="hidden md:flex w-56 shrink-0 border-r border-border/50 flex-col overflow-hidden bg-panel">
        <div className="panel-header shrink-0">
          <span className="text-sm font-medium">Files</span>
        </div>
        <div className="flex-1 overflow-y-auto">
          <FileTree
            files={files}
            selectedPath={activeTab}
            onSelectFile={handleSelectFile}
            isLoading={isLoadingTree}
          />
        </div>
      </div>

      {/* 2. Mobile File Tree Drawer Overlay (< md) */}
      {isMobileFileTreeOpen && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          {/* Backdrop */}
          <div 
            className="fixed inset-0 bg-background/80 backdrop-blur-sm transition-opacity"
            onClick={() => setIsMobileFileTreeOpen(false)}
          />

          {/* Drawer content */}
          <div className="relative w-72 max-w-[85vw] h-full bg-panel border-r border-border/50 flex flex-col shadow-2xl z-10 animate-fade-in">
            <div className="h-12 border-b border-border/50 flex items-center justify-between px-4">
              <div className="flex items-center gap-2">
                <Folder className="w-4 h-4 text-primary" />
                <span className="text-sm font-semibold">Project Files</span>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsMobileFileTreeOpen(false)}
                className="h-8 w-8 text-muted-foreground hover:text-foreground"
              >
                <X className="w-4 h-4" />
              </Button>
            </div>
            <div className="flex-1 overflow-y-auto p-2">
              <FileTree
                files={files}
                selectedPath={activeTab}
                onSelectFile={handleSelectFile}
                isLoading={isLoadingTree}
              />
            </div>
          </div>
        </div>
      )}

      {/* 3. Code Editor with Tabs */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* File Tabs & Mobile File Button */}
        <div className="flex items-center border-b border-border/50 bg-panel shrink-0 min-w-0">
          {/* Mobile File Browser Toggle Button */}
          <button
            onClick={() => setIsMobileFileTreeOpen(true)}
            className="md:hidden flex items-center gap-1.5 px-3 py-2 text-xs font-medium border-r border-border/40 text-muted-foreground hover:text-foreground hover:bg-muted/30 shrink-0"
            title="Browse Files"
          >
            <Folder className="w-3.5 h-3.5 text-primary" />
            <span>Files</span>
          </button>

          {/* Tabs */}
          <div className="flex-1 min-w-0 overflow-x-auto scrollbar-none">
            <FileTabs
              openTabs={openTabs}
              activeTab={activeTab}
              onSelectTab={handleSelectTab}
              onCloseTab={handleCloseTab}
            />
          </div>
        </div>
        
        {/* Editor */}
        <div className="flex-1 overflow-hidden">
          <CodeEditor
            content={fileContent}
            filePath={activeTab}
            isLoading={isLoadingFile}
          />
        </div>
      </div>
    </div>
  );
}
