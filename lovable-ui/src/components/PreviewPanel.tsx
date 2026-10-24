import { useState, useEffect } from "react";
import { Play, Loader2, ExternalLink, RefreshCw, Globe, AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { api, PREVIEW_URL_KEY } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";

import { RuntimeErrorAlert, RuntimeError } from "@/components/RuntimeErrorAlert";

interface PreviewPanelProps {
  projectId: string;
  runtimeError: RuntimeError | null;
  onDismiss: () => void;
  onFix: (error: RuntimeError) => void;
}

export function PreviewPanel({ projectId, runtimeError, onDismiss, onFix }: PreviewPanelProps) {
  const getProjectPreviewKey = (id: string) => `${PREVIEW_URL_KEY}_${id}`;

  const [previewUrl, setPreviewUrl] = useState<string | null>(() => {
    return localStorage.getItem(getProjectPreviewKey(projectId));
  });
  const [isDeploying, setIsDeploying] = useState(false);
  const [isIframeLoading, setIsIframeLoading] = useState(true);
  const { toast } = useToast();

  const isMixedContent =
    typeof window !== "undefined" &&
    window.location.protocol === "https:" &&
    !!previewUrl &&
    previewUrl.startsWith("http:");

  // Reload previewUrl whenever projectId changes
  useEffect(() => {
    const url = localStorage.getItem(getProjectPreviewKey(projectId));
    setPreviewUrl(url);
    if (url) {
      setIsIframeLoading(true);
    }
  }, [projectId]);

  // Store previewUrl in localStorage when it changes
  useEffect(() => {
    if (previewUrl) {
      localStorage.setItem(getProjectPreviewKey(projectId), previewUrl);
      setIsIframeLoading(true);
    }
  }, [previewUrl, projectId]);

  const handleSwitchToHttp = () => {
    try {
      const token = localStorage.getItem("auth_token");
      const userInfo = localStorage.getItem("user_info");
      const target = new URL(window.location.href.replace(/^https:/, "http:"));
      if (token) target.searchParams.set("__auth_token", token);
      if (userInfo) target.searchParams.set("__user_info", userInfo);
      window.location.replace(target.toString());
    } catch (e) {
      window.location.replace(window.location.href.replace(/^https:/, "http:"));
    }
  };

  const handleDeploy = async () => {
    setIsDeploying(true);

    try {
      const response = await api.deploy(projectId);
      setPreviewUrl(response.previewUrl);
      setIsIframeLoading(true);
      toast({
        title: "Deployment successful",
        description: "Your preview is now ready",
      });
    } catch (error) {
      toast({
        title: "Deployment failed",
        description: error instanceof Error ? error.message : "Something went wrong",
        variant: "destructive",
      });
    } finally {
      setIsDeploying(false);
    }
  };

  const handleRefresh = () => {
    setIsIframeLoading(true);
    const iframe = document.querySelector("iframe");
    if (iframe) {
      iframe.src = iframe.src;
    }
  };

  return (
    <div className="flex flex-col h-full bg-background">
      {/* URL Bar */}
      <div className="h-12 shrink-0 flex items-center gap-2 px-3 border-b border-border/50 bg-panel">
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            onClick={handleRefresh}
            disabled={!previewUrl}
            className="h-7 w-7 text-muted-foreground hover:text-foreground"
            title="Refresh preview"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </Button>
        </div>

        <div className="flex-1 flex items-center h-8 px-2.5 sm:px-3 rounded-md bg-muted/50 text-xs sm:text-sm text-muted-foreground min-w-0">
          <Globe className="w-3.5 h-3.5 mr-1.5 sm:mr-2 shrink-0" />
          <span className="truncate">
            {previewUrl || "Click 'Run Preview' to deploy"}
          </span>
        </div>

        <div className="flex items-center gap-1 shrink-0">
          {previewUrl && (
            <Button
              variant="ghost"
              size="icon"
              onClick={() => window.open(previewUrl, "_blank")}
              className="h-7 w-7 text-muted-foreground hover:text-foreground shrink-0"
              title="Open in new tab"
            >
              <ExternalLink className="w-3.5 h-3.5" />
            </Button>
          )}
          <Button
            onClick={handleDeploy}
            disabled={isDeploying}
            size="sm"
            className="h-7 px-2.5 sm:px-3 bg-primary hover:bg-primary/90 text-xs font-medium shrink-0"
          >
            {isDeploying ? (
              <>
                <Loader2 className="w-3 h-3 mr-1 animate-spin" />
                <span className="hidden sm:inline">Deploying...</span>
                <span className="sm:hidden">...</span>
              </>
            ) : (
              <>
                <Play className="w-3 h-3 mr-1" />
                <span>Run<span className="hidden sm:inline"> Preview</span></span>
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Preview Area */}
      <div className="flex-1 min-h-0 relative bg-[#1a1a1a]">
        {previewUrl ? (
          <>
            <iframe
              key={previewUrl}
              src={previewUrl}
              className="w-full h-full border-0 absolute inset-0 bg-background"
              title="Preview"
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
              onLoad={() => setIsIframeLoading(false)}
            />
            {isIframeLoading && !isMixedContent && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-background/80 backdrop-blur-sm pointer-events-none z-10">
                <Loader2 className="w-8 h-8 text-primary animate-spin mb-2" />
                <span className="text-xs text-muted-foreground">Loading preview...</span>
              </div>
            )}
            {isMixedContent && (
              <div className="absolute top-2 left-2 right-2 p-3 rounded-lg bg-card/95 border border-amber-500/40 shadow-lg flex flex-col sm:flex-row items-center justify-between gap-2.5 z-20 text-xs">
                <div className="flex items-center gap-2 text-amber-500 min-w-0">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span className="truncate">Preview blocked by HTTPS. Switch to HTTP mode for live embedded preview.</span>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <Button
                    size="sm"
                    variant="default"
                    onClick={handleSwitchToHttp}
                    className="h-7 px-2.5 text-xs bg-amber-600 hover:bg-amber-700 text-white font-medium"
                  >
                    Switch to HTTP
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => window.open(previewUrl, "_blank")}
                    className="h-7 px-2.5 text-xs border-amber-500/30 text-foreground"
                  >
                    Open in Tab
                  </Button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-center p-8">
            <div className="w-16 h-16 rounded-xl bg-muted/20 flex items-center justify-center mb-4">
              <Globe className="w-8 h-8 text-muted-foreground/50" />
            </div>
            <p className="text-sm text-muted-foreground">
              No preview available yet
            </p>
          </div>
        )}
      </div>

      {/* Error Alert Overlay - Inside the Preview Panel */}
      <RuntimeErrorAlert
        error={runtimeError}
        onDismiss={onDismiss}
        onFix={onFix}
      />
    </div>
  );
}
