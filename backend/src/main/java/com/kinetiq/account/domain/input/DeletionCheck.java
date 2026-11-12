package com.kinetiq.account.domain.input;

/**
 * What a person needs to know before deleting their account: how many clients a trainer still has (0 for a client),
 * and a client's trainer's name, which the confirmation names ({@code null} for a trainer).
 */
public record DeletionCheck(int clients, String trainerName) {}
