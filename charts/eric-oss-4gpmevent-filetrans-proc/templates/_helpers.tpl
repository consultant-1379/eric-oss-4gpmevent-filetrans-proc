{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Define upper limit for TerminationGracePeriodSeconds
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.terminationGracePeriodSeconds" -}}
  {{- if .Values.terminationGracePeriodSeconds -}}
    {{- toYaml .Values.terminationGracePeriodSeconds -}}
  {{- end -}}
{{- end -}}

{{/*
Define tolerations to comply with DR-D1120-060
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.tolerations" -}}
  {{- if .Values.tolerations -}}
    {{- toYaml .Values.tolerations -}}
  {{- end -}}
{{- end -}}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-4gpmevent-filetrans-proc.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{- define "eric-oss-4gpmevent-filetrans-proc.registryImagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if index .Values "imageCredentials" "eric-oss-4gpmevent-filetrans-proc" "registry" -}}
            {{- if index .Values "imageCredentials" "eric-oss-4gpmevent-filetrans-proc" "registry" "imagePullPolicy" -}}
                {{- $globalRegistryPullPolicy = index .Values "imageCredentials" "eric-oss-4gpmevent-filetrans-proc" "registry" "imagePullPolicy" -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}

{{- define "eric-oss-4gpmevent-filetrans-proc.mainImagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-4gpmevent-filetrans-proc" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-4gpmevent-filetrans-proc" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-4gpmevent-filetrans-proc" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-4gpmevent-filetrans-proc" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
              {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if (index .Values "imageCredentials" "eric-oss-4gpmevent-filetrans-proc") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-4gpmevent-filetrans-proc" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-4gpmevent-filetrans-proc" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-4gpmevent-filetrans-proc" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- $imagePath := printf "%s/%s/%s:%s" $registryUrl $repoPath $name $tag -}}
    {{- print (regexReplaceAll "[/]+" $imagePath "/") -}}
{{- end -}}

{{/*
This helper defines the script for terminating the side car container by job container.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-sidecar-quit" }}
{{- if eq (include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" .) "true" }}
curl -X POST http://127.0.0.1:15020/quitquitquit;
{{- end -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.standard-labels" }}
app.kubernetes.io/name: {{ include "eric-oss-4gpmevent-filetrans-proc.name" . }}
helm.sh/chart: {{ include "eric-oss-4gpmevent-filetrans-proc.chart" . }}
{{ include "eric-oss-4gpmevent-filetrans-proc.selectorLabels" . }}
app.kubernetes.io/version: {{ include "eric-oss-4gpmevent-filetrans-proc.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if eq .Values.global.fsGroup.namespace true -}}
          # The 'default' defined in the Security Policy will be used.
        {{- else -}}
          10000
      {{- end -}}
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-4gpmevent-filetrans-proc.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-4gpmevent-filetrans-proc.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Annotations for Product Name and Product Number (DR-D1121-064).
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end -}}

{{/*
Prometheus scrape annotations DR-D470223-010
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.prometheus-scrape-common" -}}
prometheus.io/scrape-port: {{ .Values.service.port | quote }}
prometheus.io/scrape-path: {{ .Values.prometheus.path | quote }}
prometheus.io/scrape-interval: {{ .Values.prometheus.interval | quote }}
{{- end -}}

{{/*
Prometheus Pod scrape annotations DR-D470223-010
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.prometheus-scrape-pod" -}}
{{- include "eric-oss-4gpmevent-filetrans-proc.prometheus-scrape-common" . }}
prometheus.io/scrape-role: "pod"
{{- end -}}

{{/*
Define network policy pod selector rules for ingress DR-D1125-059, DR-D1125-050, DR-D1125-052, DR-D1125-054
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.netpol-ingress-pod-selector-rules" -}}
    {{- if (index .Values "networkPolicy") -}}
        {{- if (index .Values "networkPolicy" "podSelector") -}}
- from:
            {{- range (index .Values "networkPolicy" "podSelector") }}
                {{- $label := .label -}}
                {{- if .ingress -}}
                    {{- range .ingress }}
                        {{- if .value }}
  - podSelector:
      matchLabels:
        {{ $label }}: {{ .value }}
                        {{- end -}}
                    {{- end -}}
                {{- end -}}
            {{- end -}}
            {{- range (index .Values "networkPolicy" "podSelector") }}
                {{- $label := .label -}}
                {{- if .ingress }}
  ports:
                    {{- range .ingress }}
    - port: {{ .port }}
      protocol: {{ .protocol }}
                    {{- end -}}
                {{- end -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{/*
Define network policy egress port rules for egress DR-D1125-059, DR-D1125-050, DR-D1125-052, DR-D1125-054
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.netpol-egress-port-rules" -}}
    {{- if (index .Values "networkPolicy") -}}
        {{- if (index .Values "networkPolicy" "port") -}}
            {{- if (index .Values "networkPolicy" "port" "egress") -}}
- to:
  - ipBlock:
      cidr: 0.0.0.0/0
  ports:
                {{- range (index .Values "networkPolicy" "port" "egress") }}
  - port: {{ .port }}
    protocol: {{ .protocol }}
                {{- end -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.nodeSelector" -}}
{{- $globalValue := (dict) -}}
{{- if .Values.global -}}
    {{- if .Values.global.nodeSelector -}}
      {{- $globalValue = .Values.global.nodeSelector -}}
    {{- end -}}
{{- end -}}
{{- if .Values.nodeSelector -}}
  {{- range $key, $localValue := .Values.nodeSelector -}}
    {{- if hasKey $globalValue $key -}}
         {{- $Value := index $globalValue $key -}}
         {{- if ne $Value $localValue -}}
           {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
         {{- end -}}
     {{- end -}}
    {{- end -}}
    nodeSelector: {{- toYaml (merge $globalValue .Values.nodeSelector) | trim | nindent 2 -}}
{{- else -}}
  {{- if not ( empty $globalValue ) -}}
    nodeSelector: {{- toYaml $globalValue | trim | nindent 2 -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create a user defined label (DR-D1121-060)
*/}}
{{ define "eric-oss-4gpmevent-filetrans-proc.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-4gpmevent-filetrans-proc.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-060)
*/}}
{{ define "eric-oss-4gpmevent-filetrans-proc.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-oss-4gpmevent-filetrans-proc.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Merged labels for Default, which includes Standard and Config (DR-D1121-060)
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.labels" -}}
  {{- $standard := include "eric-oss-4gpmevent-filetrans-proc.standard-labels" . | fromYaml -}}
  {{- $config := include "eric-oss-4gpmevent-filetrans-proc.config-labels" . | fromYaml -}}
  {{- include "eric-oss-4gpmevent-filetrans-proc.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config)) | trim }}
{{- end -}}

{{/*
Merged annotations for Default, which includes productInfo and config (DR-D1121-060)
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.annotations" -}}
  {{- $productInfo := include "eric-oss-4gpmevent-filetrans-proc.product-info" . | fromYaml -}}
  {{- $prometheusAnn := include "eric-oss-4gpmevent-filetrans-proc.prometheus" . | fromYaml -}}
  {{- $config := include "eric-oss-4gpmevent-filetrans-proc.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-4gpmevent-filetrans-proc.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $prometheusAnn $config)) | trim }}
{{- end -}}

{{/*
Create container level annotations (DR-1123-127)
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.container-annotations" }}
    {{- if .Values.appArmorProfile -}}
    {{- $appArmorValue := .Values.appArmorProfile.type -}}
        {{- if .Values.appArmorProfile.type -}}
            {{- if eq .Values.appArmorProfile.type "localhost" -}}
                {{- $appArmorValue = printf "%s/%s" .Values.appArmorProfile.type .Values.appArmorProfile.localhostProfile }}
            {{- end}}
container.apparmor.security.beta.kubernetes.io/eric-oss-4gpmevent-filetrans-proc: {{ $appArmorValue | quote }}
        {{- end}}
    {{- end}}
{{- end}}

{{/*
Seccomp profile section (DR-1123-128) [To be verified later with "RuntimeDefault" in values.yaml]
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.seccomp-profile" }}
    {{- if .Values.seccompProfile }}
      {{- if .Values.seccompProfile.type }}
          {{- if eq .Values.seccompProfile.type "Localhost" }}
              {{- if .Values.seccompProfile.localhostProfile }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
            {{- end }}
          {{- else }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
          {{- end }}
        {{- end }}
      {{- end }}
{{- end }}

{{/*
Define role kind to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.securityPolicyRoleKind" -}}
{{- $rolekind := "" -}}
{{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
        {{- if .Values.global.securityPolicy.rolekind -}}
            {{- $rolekind = .Values.global.securityPolicy.rolekind -}}
            {{- if and (ne $rolekind "Role") (ne $rolekind "ClusterRole") -}}
              {{- printf "For global.securityPolicy.rolekind only \"Role\", \"ClusterRole\" or \"\" is allowed as values." | fail -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $rolekind -}}
{{- end -}}

{{/*
Define RoleName to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.securityPolicyRolename" -}}
{{- $rolename := (include "eric-oss-4gpmevent-filetrans-proc.name" .) -}}
{{- if .Values.securityPolicy -}}
    {{- if .Values.securityPolicy.rolename -}}
        {{- $rolename = .Values.securityPolicy.rolename -}}
    {{- end -}}
{{- end -}}
{{- $rolename -}}
{{- end -}}

{{/*
Define RolebindingName to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.securityPolicy.rolebindingName" -}}
{{- $rolekind := "" -}}
{{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
        {{- if .Values.global.securityPolicy.rolekind -}}
            {{- $rolekind = .Values.global.securityPolicy.rolekind -}}
            {{- if (eq $rolekind "Role") -}}
               {{- print (include "eric-oss-4gpmevent-filetrans-proc.serviceAccountName" .) "-r-" (include "eric-oss-4gpmevent-filetrans-proc.securityPolicyRolename" .) "-sp" -}}
            {{- else if (eq $rolekind "ClusterRole") -}}
               {{- print (include "eric-oss-4gpmevent-filetrans-proc.serviceAccountName" .) "-c-" (include "eric-oss-4gpmevent-filetrans-proc.securityPolicyRolename" .) "-sp" -}}
            {{- end }}
        {{- end }}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
    Define supplementalGroups (DR-D1123-135)
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.supplementalGroups" -}}
  {{- $globalGroups := (list) -}}
  {{- if ( (((.Values).global).podSecurityContext).supplementalGroups) }}
    {{- $globalGroups = .Values.global.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $localGroups := (list) -}}
  {{- if ( ((.Values).podSecurityContext).supplementalGroups) -}}
    {{- $localGroups = .Values.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $mergedGroups := (list) -}}
  {{- if $globalGroups -}}
    {{- $mergedGroups = $globalGroups -}}
  {{- end -}}
  {{- if $localGroups -}}
    {{- $mergedGroups = concat $globalGroups $localGroups | uniq -}}
  {{- end -}}
  {{- if $mergedGroups -}}
    supplementalGroups: {{- toYaml $mergedGroups | nindent 8 -}}
  {{- end -}}
  {{- /*Do nothing if both global and local groups are not set */ -}}
{{- end -}}

{{/*----------------------------------- Service mesh functions ----------------------------------*/}}
{{/*
DR-D470217-011 This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-inject" }}
{{- if eq (include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
{{- else -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}
{{/*
DR-D470217-007-AD This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}

{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-version" }}
{{- if eq (include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" .) "true" }}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.annotations -}}
        {{ .Values.global.serviceMesh.annotations | toYaml }}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}
{{/*
check global.security.tls.enabled
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.global-security-tls-enabled" -}}
{{- if  .Values.global -}}
  {{- if  .Values.global.security -}}
    {{- if  .Values.global.security.tls -}}
      {{- .Values.global.security.tls.enabled | toString -}}
    {{- else -}}
      {{- "false" -}}
    {{- end -}}
  {{- else -}}
    {{- "false" -}}
  {{- end -}}
{{- else -}}
  {{- "false" -}}
{{- end -}}
{{- end -}}
{{/*
Define kafka bootstrap server
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.kafka-bootstrap-server" -}}
{{- $kafkaBootstrapServer := "" -}}
{{- $serviceMesh := ( include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" . ) -}}
{{- $tls := ( include "eric-oss-4gpmevent-filetrans-proc.global-security-tls-enabled" . ) -}}
{{- if and (eq $serviceMesh "true") (eq $tls "true") -}}
    {{- $kafkaBootstrapServer = .Values.spring.kafka.bootstrapServersTls -}}
{{ else }}
    {{- $kafkaBootstrapServer = .Values.spring.kafka.bootstrapServer -}}
{{ end }}
{{- if .Values.global -}}
  {{- if .Values.global.dependentServices -}}
    {{- if .Values.global.dependentServices.dmm -}}
        {{- if and (eq $serviceMesh "true") (eq $tls "true") -}}
           {{- if index .Values.global.dependentServices.dmm "kafka-bootstrapServersTls" -}}
               {{- $kafkaBootstrapServer = index .Values.global.dependentServices.dmm "kafka-bootstrapServersTls" -}}
           {{- end -}}
        {{ else }}
           {{- if index .Values.global.dependentServices.dmm "kafka-bootstrap" -}}
               {{- $kafkaBootstrapServer = index .Values.global.dependentServices.dmm "kafka-bootstrap" -}}
           {{- end -}}
        {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $kafkaBootstrapServer -}}
{{- end -}}
{{/*
This helper defines which out-mesh services will be reached by this one (DR-D470217-001).
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-ism2osm-labels" -}}
{{- if eq (include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-oss-4gpmevent-filetrans-proc.global-security-tls-enabled" .) "true" }}
eric-oss-dmm-kf-op-sz-kafka-ism-access: "true"
  {{- end }}
{{- end }}
{{- end -}}
{{/*
This helper defines the annotation for define service mesh volume.
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-volume" -}}
{{- if and (eq (include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" .) "true") (eq (include "eric-oss-4gpmevent-filetrans-proc.global-security-tls-enabled" .) "true") }}
sidecar.istio.io/userVolume: '{"{{ include "eric-oss-4gpmevent-filetrans-proc.name" . }}-kafka-certs-tls":{"secret":{"secretName":"{{ include "eric-oss-4gpmevent-filetrans-proc.name" . }}-kafka-secret","optional":true}},"{{ include "eric-oss-4gpmevent-filetrans-proc.name" . }}-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}}}'
sidecar.istio.io/userVolumeMount: '{"{{ include "eric-oss-4gpmevent-filetrans-proc.name" . }}-kafka-certs-tls":{"mountPath":"/etc/istio/tls/eric-oss-dmm-kf-op-sz-kafka-bootstrap/","readOnly":true},"{{ include "eric-oss-4gpmevent-filetrans-proc.name" . }}-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
{{ end }}
{{- end -}}

{{- define "eric-oss-4gpmevent-filetrans-proc.kafka-cluster-label" }}
strimzi.io/cluster: {{  .Values.spring.kafka.clusterName  }}
{{- end -}}

{{/*
Create Service Mesh Egress enabling option
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-egress-enabled" }}
  {{- $globalMeshEgressEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.egress -}}
        {{- $globalMeshEgressEnabled = .Values.global.serviceMesh.egress.enabled -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEgressEnabled -}}
{{- end -}}
{{/*
This helper defines permissive network policy for external access
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.service-mesh-egress-gateway-access-label" }}
{{- $serviceMesh := include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" . | trim -}}
{{- $serviceMeshEgress := include "eric-oss-4gpmevent-filetrans-proc.service-mesh-egress-enabled" . | trim -}}
{{- if and (eq $serviceMesh "true") (eq $serviceMeshEgress "true") -}}
service-mesh-egress-gateway-access: "true"
{{- end -}}
{{- end -}}

{{- define "eric-oss-4gpmevent-filetrans-proc.istio-proxy-config-annotation" }}
{{- if eq (include "eric-oss-4gpmevent-filetrans-proc.service-mesh-enabled" .) "true" }}
proxy.istio.io/config: '{ "holdApplicationUntilProxyStarts": true }'
{{- end -}}
{{- end -}}

{{- define "eric-oss-4gpmevent-filetrans-proc.egress-bandwidth-label" }}
    {{- if .Values.bandwidth.maxEgressRate }}
        app.kubernetes.io/egress-bandwidth: {{ .Values.bandwidth.maxEgressRate | quote }}
    {{- end }}
{{- end -}}

{/*
Define JVM heap size
*/}}
{{- define "eric-oss-4gpmevent-filetrans-proc.jvmHeapSettings" -}}
    {{- $initRAM := "" -}}
    {{- $maxRAM := "" -}}
    {{/*
       ramLimit is set by default to 1.0, this is if the service is set to use anything less than M/Mi
       Rather than trying to cover each type of notation,
       if a user is using anything less than M/Mi then the assumption is its less than the cutoff of 1.3GB
       */}}
    {{- $ramLimit := 1.0 -}}
    {{- $ramComparison := 1.3 -}}

    {{- if not (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory") -}}
        {{- fail "memory limit for eric-oss-4gpmevent-filetrans-proc is not specified" -}}
    {{- end -}}

    {{- if (hasSuffix "Gi" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "Gi" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "G" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "G" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "Mi" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "Mi" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory") | float64) 1000) | float64  -}}
    {{- else if (hasSuffix "M" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "M" (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "limits" "memory") | float64) 1000) | float64  -}}
    {{- end -}}

    {{- if (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "jvm") -}}
        {{- if (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "jvm" "initialMemoryAllocationPercentage") -}}
            {{- $initRAM = index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "jvm" "initialMemoryAllocationPercentage" | float64 -}}
            {{- $initRAM = printf "-XX:InitialRAMPercentage=%f" $initRAM -}}
        {{- else -}}
            {{- fail "initialMemoryAllocationPercentage not set" -}}
        {{- end -}}
        {{- if and (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "jvm" "smallMemoryAllocationMaxPercentage") (index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "jvm" "largeMemoryAllocationMaxPercentage") -}}
            {{- if lt $ramLimit $ramComparison -}}
                {{- $maxRAM =index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "jvm" "smallMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- else -}}
                {{- $maxRAM = index .Values "resources" "eric-oss-4gpmevent-filetrans-proc" "jvm" "largeMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- end -}}
        {{- else -}}
            {{- fail "smallMemoryAllocationMaxPercentage | largeMemoryAllocationMaxPercentage not set" -}}
        {{- end -}}
    {{- else -}}
        {{- fail "jvm heap percentages are not set" -}}
    {{- end -}}
{{- printf "%s %s" $initRAM $maxRAM -}}
{{- end -}}
