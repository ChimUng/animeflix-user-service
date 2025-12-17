package com.animeflix.animecatalogservice.mapper;

import com.animeflix.animecatalogservice.DTO.AnimeDetailResponse;
import com.animeflix.animecatalogservice.DTO.AnimeResponse;
import com.animeflix.animecatalogservice.Entity.Anime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AnimeMapper {

    AnimeResponse toResponse(Anime anime);

    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "trailer", source = "trailer")
    @Mapping(target = "studios", source = "studios", qualifiedByName = "mapStudios")
    @Mapping(target = "relations", source = "relations", qualifiedByName = "mapRelations")
    @Mapping(target = "characters", source = "characters", qualifiedByName = "mapCharacters")
    @Mapping(target = "recommendations", source = "recommendations", qualifiedByName = "mapRecommendations")
    AnimeDetailResponse toDetailResponse(Anime anime);

    @Named("mapStudios")
    default List<AnimeDetailResponse.StudioDTO> mapStudios(Anime.StudioConnection connection) {
        if (connection == null || connection.getNodes() == null) return Collections.emptyList();
        return connection.getNodes().stream()
                .map(node -> new AnimeDetailResponse.StudioDTO(node.getId(), node.getName()))
                .collect(Collectors.toList());
    }

    @Named("mapRelations")
    default List<AnimeDetailResponse.RelationDTO> mapRelations(Anime.RelationConnection connection) {
        if (connection == null || connection.getEdges() == null) return Collections.emptyList();
        return connection.getEdges().stream()
                .filter(edge -> edge.getNode() != null)
                .map(edge -> {
                    Anime.RelationNode node = edge.getNode();

                    // ✅ SAFE MAPPING - Chỉ set field nào có giá trị
                    AnimeResponse.Title.TitleBuilder titleBuilder = AnimeResponse.Title.builder();
                    if (node.getTitle() != null) {
                        if (node.getTitle().getRomaji() != null)
                            titleBuilder.romaji(node.getTitle().getRomaji());
                        if (node.getTitle().getEnglish() != null)
                            titleBuilder.english(node.getTitle().getEnglish());
                        if (node.getTitle().getUserPreferred() != null)
                            titleBuilder.userPreferred(node.getTitle().getUserPreferred());
                        if (node.getTitle().getNativeTitle() != null)
                            titleBuilder.nativeTitle(node.getTitle().getNativeTitle());
                    }

                    AnimeResponse.CoverImage.CoverImageBuilder coverBuilder = AnimeResponse.CoverImage.builder();
                    if (node.getCoverImage() != null) {
                        if (node.getCoverImage().getLarge() != null)
                            coverBuilder.large(node.getCoverImage().getLarge());
                        if (node.getCoverImage().getExtraLarge() != null)
                            coverBuilder.extraLarge(node.getCoverImage().getExtraLarge());
                        if (node.getCoverImage().getColor() != null)
                            coverBuilder.color(node.getCoverImage().getColor());
                    }

                    return AnimeDetailResponse.RelationDTO.builder()
                            .relationType(edge.getRelationType())
                            .id(node.getId())
                            .title(titleBuilder.build())
                            .coverImage(coverBuilder.build())
                            .format(node.getFormat())
                            .status(node.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Named("mapCharacters")
    default List<AnimeDetailResponse.CharacterDTO> mapCharacters(Anime.CharacterConnection connection) {
        if (connection == null || connection.getEdges() == null) return Collections.emptyList();
        return connection.getEdges().stream()
                .limit(12)
                .map(edge -> {
                    AnimeDetailResponse.CharacterDTO dto = new AnimeDetailResponse.CharacterDTO();
                    dto.setRole(edge.getRole());

                    if (edge.getNode() != null) {
                        dto.setId(edge.getNode().getId());
                        if (edge.getNode().getName() != null) {
                            dto.setName(edge.getNode().getName().getFull());
                        }
                        if (edge.getNode().getImage() != null) {
                            dto.setImage(edge.getNode().getImage().getLarge());
                        }
                    }

                    if (edge.getVoiceActorRoles() != null && !edge.getVoiceActorRoles().isEmpty()) {
                        Anime.VoiceActor va = edge.getVoiceActorRoles().get(0).getVoiceActor();
                        if (va != null) {
                            if (va.getName() != null) dto.setVoiceActorName(va.getName().getFull());
                            if (va.getImage() != null) dto.setVoiceActorImage(va.getImage().getLarge());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Named("mapRecommendations")
    default List<AnimeResponse> mapRecommendations(Anime.RecommendationConnection connection) {
        if (connection == null || connection.getNodes() == null) return Collections.emptyList();

        return connection.getNodes().stream()
                .map(node -> {
                    var media = node.getMediaRecommendation();
                    if (media == null) return null;

                    AnimeResponse res = new AnimeResponse();
                    res.setId(media.getId());
                    res.setStatus(media.getStatus());
                    res.setFormat(media.getFormat());
                    res.setEpisodes(media.getEpisodes());

                    // ✅ SAFE MAPPING cho Title
                    if (media.getTitle() != null) {
                        AnimeResponse.Title.TitleBuilder titleBuilder = AnimeResponse.Title.builder();
                        if (media.getTitle().getRomaji() != null)
                            titleBuilder.romaji(media.getTitle().getRomaji());
                        if (media.getTitle().getEnglish() != null)
                            titleBuilder.english(media.getTitle().getEnglish());
                        if (media.getTitle().getUserPreferred() != null)
                            titleBuilder.userPreferred(media.getTitle().getUserPreferred());

                        res.setTitle(titleBuilder.build());
                    }

                    // ✅ SAFE MAPPING cho CoverImage
                    if (media.getCoverImage() != null) {
                        AnimeResponse.CoverImage.CoverImageBuilder coverBuilder = AnimeResponse.CoverImage.builder();
                        if (media.getCoverImage().getLarge() != null)
                            coverBuilder.large(media.getCoverImage().getLarge());
                        if (media.getCoverImage().getExtraLarge() != null)
                            coverBuilder.extraLarge(media.getCoverImage().getExtraLarge());
                        if (media.getCoverImage().getColor() != null)
                            coverBuilder.color(media.getCoverImage().getColor());

                        res.setCoverImage(coverBuilder.build());
                    }

                    return res;
                })
                .filter(r -> r != null) // Bỏ qua null
                .limit(5)
                .collect(Collectors.toList());
    }
}